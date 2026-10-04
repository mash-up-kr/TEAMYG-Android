package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle

internal enum class ToppingPanelBackAction {
    Ignore,
    ClosePanel,
    ShowQuitDialog,
}

/** 테두리 패널이 있는 화면이 시스템 뒤로가기를 받았을 때 할 일. 로딩 중에는 패널도 닫지 않는다 */
internal fun toppingPanelBackAction(
    isLoading: Boolean,
    isBorderPanelOpen: Boolean,
): ToppingPanelBackAction = when {
    isLoading -> ToppingPanelBackAction.Ignore
    isBorderPanelOpen -> ToppingPanelBackAction.ClosePanel
    else -> ToppingPanelBackAction.ShowQuitDialog
}

/**
 * 패널이 열린 동안 화면은 토핑을 저장된 자리가 아닌 곳에 보여 준다. 그 상태에서 받은 이동량을
 * 저장된 자리에 더하면 닫았을 때 토핑이 엉뚱한 데로 가 있다.
 */
internal fun ignoresToppingTransform(isBorderPanelOpen: Boolean): Boolean = isBorderPanelOpen

/** 패널 슬라이더가 보여 줄 굵기. 테두리가 없으면 색을 고르기 전에 맞춰 둔 [pendingWidthDp] 다 */
internal fun resolvePanelBorderWidthDp(
    border: ToppingBorderStyle?,
    pendingWidthDp: Float,
): Float = border?.widthDp ?: pendingWidthDp

internal fun clampPanelBorderWidthDp(widthDp: Float): Float = widthDp.coerceIn(TOPPING_BORDER_WIDTH_RANGE_DP)

/**
 * 패널에서 색을 골랐을 때의 테두리.
 *
 * @param colorArgb `null` 은 테두리 없음
 * @param panelWidthDp 패널이 지금 보여 주는 굵기 — [resolvePanelBorderWidthDp]
 */
internal fun panelBorderForColor(
    colorArgb: Int?,
    panelWidthDp: Float,
): ToppingBorderStyle? = colorArgb?.let { argb -> ToppingBorderStyle(argb, panelWidthDp) }

/** 테두리가 없으면 그대로 없다 — 굵기만으로 테두리를 만들지 않는다 */
internal fun ToppingBorderStyle?.withPanelWidth(widthDp: Float): ToppingBorderStyle? = this?.copy(widthDp = widthDp)
