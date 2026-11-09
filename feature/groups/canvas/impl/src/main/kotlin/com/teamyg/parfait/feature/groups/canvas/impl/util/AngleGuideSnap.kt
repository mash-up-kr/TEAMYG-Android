package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.AngleGuideResult
import kotlin.math.abs
import kotlin.math.roundToInt

/** 가이드·스냅이 켜지는 각도 거리 임계값 */
internal const val ANGLE_GUIDE_THRESHOLD_DEGREES = 3f

/** 스냅 대상 각도 간격 — 0/90/180/270도 */
private const val ANGLE_GUIDE_STEP_DEGREES = 90f

/**
 * 회전 각도가 0/90/180/270도 중 가장 가까운 배수 기준 ±[thresholdDegrees] 이내면 스냅한다.
 * [rawDegrees] 는 누적된(360도로 접지 않은) 원값이다
 */
internal fun resolveAngleGuide(
    rawDegrees: Float,
    thresholdDegrees: Float = ANGLE_GUIDE_THRESHOLD_DEGREES,
): AngleGuideResult {
    val nearestMultiple = (rawDegrees / ANGLE_GUIDE_STEP_DEGREES).roundToInt() * ANGLE_GUIDE_STEP_DEGREES
    val isSnapped = abs(rawDegrees - nearestMultiple) <= thresholdDegrees
    val normalized = ((nearestMultiple % 360f) + 360f) % 360f
    val isHorizontal = normalized == 0f || normalized == 180f
    return AngleGuideResult(
        isSnapped = isSnapped,
        effectiveValue = if (isSnapped) nearestMultiple else rawDegrees,
        isHorizontal = isHorizontal,
    )
}
