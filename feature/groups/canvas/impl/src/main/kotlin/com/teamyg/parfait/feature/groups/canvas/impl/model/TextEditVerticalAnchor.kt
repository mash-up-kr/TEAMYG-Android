package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.round

/** 편집 중 pin 전, 박스의 아래변을 캔버스의 이 비율 지점에 고정한다 */
private const val TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO = 0.5f

/** 편집 중 Text-Area 의 세로 고정 애니메이션 결과 */
internal class TextEditVerticalAnchor(
    val topPx: Int,
    val heightPx: Float,
    val targetHeightPx: Float,
    val budgetPx: Int,
    val onMeasured: () -> Unit,
)

/**
 * 편집 중 Text-Area 를 캔버스 상단 ~ 키보드 바로 위 사이에서 세로로 고정한다(스펙 「Text-Area」:
 * 높이 = 캔버스 높이 − 키보드 높이 − 툴바 높이). [TextEditVerticalAnchor.onMeasured] 로 "이
 * 세션에서 처음 측정됐다"는 신호를 받아야 한다. [canvasTopPx]·[rootHeightPx] 는 캔버스·루트
 * 레이아웃이 실측될 때마다 호출부가 최신값을 넘긴다.
 */
@Composable
internal fun rememberTextEditVerticalAnchor(
    density: Density,
    isEditing: Boolean,
    canvasTopPx: Float,
    rootHeightPx: Int,
    editAreaHeightPx: Int,
    textBoxHeightDp: Dp?,
    toolbarHeightPx: Int,
    maxBudgetPx: Int,
): TextEditVerticalAnchor {
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // 상단 핀 위치: 캔버스 자신의 상단(캔버스 local 좌표는 여기가 0) — 편집 중에도 텍스트가
    // 캔버스 밖(그 위 딤 영역)으로 못 올라간다
    val editTopMarginPx = 0

    // 하단 한계 위치: 키보드 위에 얹히는 툴바 바로 위 지점을 캔버스 local Y 좌표로 환산
    val keyboardTopPx = rootHeightPx - imeBottomPx - toolbarHeightPx
    val editBottomLimitPx: Int = round(keyboardTopPx - canvasTopPx).toInt()

    // 세로 예산: 캔버스 높이 중 키보드·툴바에 가려지지 않는 부분이되, 캔버스 자체 높이·디자인
    // 상한(331dp)을 넘지 않는다. editAreaHeightPx 는 캔버스가 측정되기 전 0 일 수 있어 최소 1 을 보장한다
    val editBudgetMaxPx = minOf(editAreaHeightPx, maxBudgetPx).coerceAtLeast(1)
    val editBudgetPx: Int = editBottomLimitPx.coerceIn(1, editBudgetMaxPx)

    // 매 편집 세션이 항상 가운데에서 시작하도록 강제한다
    var hasMeasuredThisEditSession by remember { mutableStateOf(false) }
    LaunchedEffect(isEditing) {
        if (isEditing) hasMeasuredThisEditSession = false
    }
    val editBoxHeightTargetPx = if (hasMeasuredThisEditSession) {
        with(density) { (textBoxHeightDp ?: 0.dp).toPx() }
    } else {
        0f
    }
    // 윗변 계산에만 예산으로 캡 건 높이를 쓴다 — 박스가 예산을 넘어 스케일로 축소되는 동안에는
    // (scaleBoxToFit 이전의) 원본 측정값이 계속 자라서, 캡이 없으면 줄바꿈마다 윗변이 상태바까지
    // 튀어 오른다. 예산에 닿은 뒤로는 제자리에서 고정되고 그 이상은 폰트 스케일로만 줄어야 한다.
    val editTopAnchorHeightPx = editBoxHeightTargetPx.coerceAtMost(editBudgetPx.toFloat())
    // 박스의 **아래변**을 캔버스의 [TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO] 지점에 고정한다
    val editNaturalTopPx: Int = round(
        editAreaHeightPx * TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO - editTopAnchorHeightPx,
    ).toInt()
    // 윗변: 아래변 고정 기준이되 상단 여백 아래로는 못 올라간다
    val editTopTargetPx = maxOf(editTopMarginPx, editNaturalTopPx)

    var editTopWasEditingLastFrame by remember { mutableStateOf(isEditing) }
    val skipEditTopAnimationThisFrame = isEditing != editTopWasEditingLastFrame
    SideEffect { editTopWasEditingLastFrame = isEditing }
    val editTopPx by animateIntAsState(
        targetValue = editTopTargetPx,
        animationSpec = if (skipEditTopAnimationThisFrame) snap() else tween(durationMillis = 90),
        label = "editTopPx",
    )
    // 윗변과 같은 타이밍으로 높이도 같이 애니메이션한다
    val editBoxHeightPx by animateFloatAsState(
        targetValue = editBoxHeightTargetPx,
        animationSpec = if (skipEditTopAnimationThisFrame) snap() else tween(durationMillis = 90),
        label = "editBoxHeightPx",
    )

    return TextEditVerticalAnchor(
        topPx = editTopPx,
        heightPx = editBoxHeightPx,
        targetHeightPx = editBoxHeightTargetPx,
        budgetPx = editBudgetPx,
        onMeasured = { hasMeasuredThisEditSession = true },
    )
}
