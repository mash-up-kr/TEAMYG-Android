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
import kotlin.math.round

/** 편집 중 Text-Area 의 세로 고정 애니메이션 결과 */
internal class TextEditVerticalAnchor(
    val topPx: Int,
    val heightPx: Float,
    val targetHeightPx: Float,
    val budgetPx: Int,
    val onMeasured: () -> Unit,
)

/** 편집 중 세로 예산과 하단 한계 위치 */
internal class TextEditBudget(
    val budgetPx: Int,
    val bottomLimitPx: Int,
)

@Composable
internal fun rememberTextEditBudget(
    density: Density,
    canvasTopPx: Float,
    rootHeightPx: Int,
    editAreaHeightPx: Int,
    toolbarHeightPx: Int,
): TextEditBudget {
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    // 하단 한계 위치: 키보드 위에 얹히는 툴바 바로 위 지점을 캔버스 local Y 좌표로 환산
    val keyboardTopPx = rootHeightPx - imeBottomPx - toolbarHeightPx
    val editBottomLimitPx: Int = round(keyboardTopPx - canvasTopPx).toInt()

    val editBudgetPx: Int = editBottomLimitPx.coerceIn(1, editAreaHeightPx.coerceAtLeast(1))

    return TextEditBudget(budgetPx = editBudgetPx, bottomLimitPx = editBottomLimitPx)
}

@Composable
internal fun rememberTextEditVerticalAnchor(
    isEditing: Boolean,
    budget: TextEditBudget,
    textBoxHeightPx: Float,
    isAtMaxHeight: Boolean,
): TextEditVerticalAnchor {
    val editBudgetPx = budget.budgetPx
    val editBottomLimitPx = budget.bottomLimitPx

    // 상단 핀 위치: 캔버스 자신의 상단(캔버스 local 좌표는 여기가 0) — 편집 중에도 텍스트가
    // 캔버스 밖(그 위 딤 영역)으로 못 올라간다
    val editTopMarginPx = 0

    // 매 편집 세션이 항상 가운데에서 시작하도록 강제한다
    var hasMeasuredThisEditSession by remember { mutableStateOf(false) }
    LaunchedEffect(isEditing) {
        if (isEditing) hasMeasuredThisEditSession = false
    }

    val editBoxHeightTargetPx = if (hasMeasuredThisEditSession) textBoxHeightPx else 0f
    val editTopAnchorHeightPx = editBoxHeightTargetPx.coerceAtMost(editBudgetPx.toFloat())

    val editTopTargetPx = if (isAtMaxHeight) {
        editTopMarginPx
    } else {
        val textAreaCenterPx = editBottomLimitPx / 2f
        val editNaturalTopPx = round(textAreaCenterPx - editTopAnchorHeightPx / 2f).toInt()
        maxOf(editTopMarginPx, editNaturalTopPx)
    }

    var editTopWasEditingLastFrame by remember { mutableStateOf(isEditing) }
    val skipEditTopAnimationThisFrame = isEditing != editTopWasEditingLastFrame
    SideEffect { editTopWasEditingLastFrame = isEditing }
    val editTopPx by animateIntAsState(
        targetValue = editTopTargetPx,
        animationSpec = if (skipEditTopAnimationThisFrame) snap() else tween(durationMillis = 90),
        label = "editTopPx",
    )
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
