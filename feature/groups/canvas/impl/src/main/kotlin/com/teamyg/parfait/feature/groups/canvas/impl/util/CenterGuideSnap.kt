package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.math.abs

/** 가이드·스냅이 켜지는 중심 거리 임계값 */
internal const val CENTER_GUIDE_THRESHOLD_DP = 6f

internal data class CenterGuideAxisResult(
    val isSnapped: Boolean,
    val effectiveValue: Float,
)

internal fun resolveCenterGuideAxisDp(
    rawCenterDp: Float,
    canvasCenterDp: Float,
    thresholdDp: Float = CENTER_GUIDE_THRESHOLD_DP,
): CenterGuideAxisResult {
    val isSnapped = abs(rawCenterDp - canvasCenterDp) <= thresholdDp
    return CenterGuideAxisResult(
        isSnapped = isSnapped,
        effectiveValue = if (isSnapped) canvasCenterDp else rawCenterDp,
    )
}

internal fun resolveCenterGuideAxisRatio(
    rawRatio: Float,
    thresholdRatio: Float,
): CenterGuideAxisResult {
    val isSnapped = abs(rawRatio - CENTER_RATIO) <= thresholdRatio
    return CenterGuideAxisResult(
        isSnapped = isSnapped,
        effectiveValue = if (isSnapped) CENTER_RATIO else rawRatio,
    )
}

private const val CENTER_RATIO = 0.5f
