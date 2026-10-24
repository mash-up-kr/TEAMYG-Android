package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
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
 * 편집 중 Text-Area 를 상태바 바로 아래 ~ 키보드 바로 위 사이에서 세로로 고정한다.
 * [TextEditVerticalAnchor.onMeasured] 로 "이 세션에서 처음 측정됐다"는 신호를 받아야 한다.
 * [canvasTopPx]·[rootHeightPx] 는 캔버스·루트 레이아웃이 실측될 때마다 호출부가 최신값을 넘긴다.
 */
@Composable
internal fun rememberTextEditVerticalAnchor(
    density: Density,
    isEditing: Boolean,
    canvasTopPx: Float,
    rootHeightPx: Int,
    editAreaHeightPx: Int,
    textBoxHeightDp: Dp?,
): TextEditVerticalAnchor {
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // 상단 핀 위치: 시스템 상태바 바로 아래 지점을 캔버스 local Y 좌표로 환산.
    val statusBarTopPx = WindowInsets.statusBars.getTop(density)
    val editTopMarginPx: Int = round(statusBarTopPx - canvasTopPx).toInt()

    // 하단 한계 위치: 키보드 바로 위 지점을 캔버스 local Y 좌표로 환산
    val keyboardTopPx = rootHeightPx - imeBottomPx
    val editBottomLimitPx: Int = round(keyboardTopPx - canvasTopPx).toInt()

    // 세로 예산: 상태바 부근 ~ 키보드 바로 위 사이의 최대 가용 영역.
    val editBudgetPx: Int = (editBottomLimitPx - editTopMarginPx).coerceAtLeast(1)

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
    // 박스의 **아래변**을 캔버스의 [TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO] 지점에 고정한다
    val editNaturalTopPx: Int = round(
        editAreaHeightPx * TEXT_TOPPING_EDIT_BOTTOM_ANCHOR_RATIO - editBoxHeightTargetPx,
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
