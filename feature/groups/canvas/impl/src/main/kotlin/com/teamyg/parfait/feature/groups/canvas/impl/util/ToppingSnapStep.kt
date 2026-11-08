package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.Dp

/** 각도 가이드 한 프레임 판정 결과. */
internal data class AngleGuideStep(
    val rawDegrees: Float,
    val hasRotated: Boolean,
    val effectiveDegrees: Float,
    val isVisible: Boolean,
    val isHorizontal: Boolean,
    val justSnapped: Boolean,
)

/**
 * 각도 가이드 한 프레임 판정. 세 토핑 화면(Place·TextInput·Arrange)이 공유한다.
 * @param oldRawDegrees 직전까지 누적된(360도로 접지 않은) 원값.
 * @param oldHasRotated 이번 제스처에서 지금까지 회전이 있었는가.
 * @param rotationDelta 이번 프레임의 회전 증분.
 */
internal fun stepAngleGuide(
    oldRawDegrees: Float,
    oldHasRotated: Boolean,
    rotationDelta: Float,
): AngleGuideStep {
    val hasRotated = oldHasRotated || rotationDelta != 0f
    val rawDegrees = oldRawDegrees + rotationDelta
    val result = resolveAngleGuide(rawDegrees)
    val isVisible = hasRotated && result.isSnapped
    val wasVisible = oldHasRotated && resolveAngleGuide(oldRawDegrees).isSnapped
    return AngleGuideStep(
        rawDegrees = rawDegrees,
        hasRotated = hasRotated,
        effectiveDegrees = if (result.isSnapped) result.effectiveValue else rawDegrees,
        isVisible = isVisible,
        isHorizontal = result.isHorizontal,
        justSnapped = isVisible && !wasVisible,
    )
}

/**
 * Dp 오프셋 기반 중앙 가이드 한 프레임 판정(Place·TextInput 공유).
 */
internal data class CenterGuideDpStep(
    val rawOffsetX: Dp,
    val rawOffsetY: Dp,
    val guideX: CenterGuideAxisResult,
    val guideY: CenterGuideAxisResult,
    val justSnapped: Boolean,
)

internal fun stepCenterGuideDp(
    oldRawOffsetX: Dp,
    oldRawOffsetY: Dp,
    panX: Dp,
    panY: Dp,
    halfWidth: Dp,
    halfHeight: Dp,
    canvasCenterXDp: Float,
    canvasCenterYDp: Float,
): CenterGuideDpStep {
    val wasSnappedX = resolveCenterGuideAxisDp((oldRawOffsetX + halfWidth).value, canvasCenterXDp).isSnapped
    val wasSnappedY = resolveCenterGuideAxisDp((oldRawOffsetY + halfHeight).value, canvasCenterYDp).isSnapped

    val rawOffsetX = oldRawOffsetX + panX
    val rawOffsetY = oldRawOffsetY + panY
    val guideX = resolveCenterGuideAxisDp((rawOffsetX + halfWidth).value, canvasCenterXDp)
    val guideY = resolveCenterGuideAxisDp((rawOffsetY + halfHeight).value, canvasCenterYDp)

    val justSnapped = (guideX.isSnapped && !wasSnappedX) || (guideY.isSnapped && !wasSnappedY)
    return CenterGuideDpStep(rawOffsetX, rawOffsetY, guideX, guideY, justSnapped)
}
