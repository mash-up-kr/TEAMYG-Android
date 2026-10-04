package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** 패널이 올라오면 토핑이 패널에 가리지 않도록 캔버스 세로 중앙에서 이만큼 위로 옮겨 보인다 */
internal val PANEL_FOCUS_LIFT: Dp = 18.5.dp

/** 패널이 열린 동안 포커스 토핑을 보여 줄 중심. 화면에만 쓰고 저장하는 위치는 바꾸지 않는다 */
internal fun panelFocusCenter(canvasSize: DpSize): DpOffset =
    DpOffset(x = canvasSize.width / 2, y = canvasSize.height / 2 - PANEL_FOCUS_LIFT)

/** 저장된 자리(0)와 [panelFocusCenter](1) 사이에서 토핑을 어디에 그릴지. 패널을 따라 움직인다 */
@Composable
internal fun animatePanelFocusFraction(isBorderPanelOpen: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (isBorderPanelOpen) 1f else 0f,
    label = "toppingPanelFocus",
)
