package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

/** 토핑이 위치·각도 가이드에 스냅될 때 붙는 모션의 길이 */
private const val TOPPING_SNAP_SETTLE_DURATION_MS = 90

/**
 * 스냅된 순간에만 [TOPPING_SNAP_SETTLE_DURATION_MS] 모션으로 붙고, 그 밖에는 손가락과 즉시 같이
 * 움직이도록 [snap] 으로 바로 반영한다. 위치(x·y) 좌표에 쓴다.
 */
@Composable
internal fun animateToppingSnapSettleDp(
    targetValue: Dp,
    isSnapping: Boolean,
): State<Dp> = animateDpAsState(
    targetValue = targetValue,
    animationSpec = if (isSnapping) tween(durationMillis = TOPPING_SNAP_SETTLE_DURATION_MS) else snap(),
    label = "toppingSnapSettleDp",
)

/** 회전 각도용 [animateToppingSnapSettleDp]. */
@Composable
internal fun animateToppingSnapSettleRotation(
    targetValue: Float,
    isSnapping: Boolean,
): State<Float> = animateFloatAsState(
    targetValue = targetValue,
    animationSpec = if (isSnapping) tween(durationMillis = TOPPING_SNAP_SETTLE_DURATION_MS) else snap(),
    label = "toppingSnapSettleRotation",
)
