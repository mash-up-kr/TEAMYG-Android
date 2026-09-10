package com.teamyg.parfait.data.utils.image

import kotlin.math.roundToInt

internal const val LUMINANCE_LEVELS = 256

private const val LOW_PERCENTILE = 0.01f
private const val HIGH_PERCENTILE = 0.99f
private const val MAX_LEVEL = LUMINANCE_LEVELS - 1

/**
 * 퍼센타일 절단 후 선형 확장 LUT. 절단점이 겹치면 항등이다.
 *
 * 전역 히스토그램 평활화를 쓰지 않는 것은 계조를 뭉개기 때문이다. 이상치만 자르고 본체의 비율은 유지한다.
 */
internal fun contrastLut(histogram: IntArray): IntArray {
    require(histogram.size == LUMINANCE_LEVELS) { "histogram must have $LUMINANCE_LEVELS levels" }

    var total = 0L
    for (count in histogram) total += count
    if (total <= 0L) return identityLut()

    val low = levelReaching(histogram, (total * LOW_PERCENTILE).toLong())
    val high = levelReaching(histogram, (total * HIGH_PERCENTILE).toLong())
    if (high <= low) return identityLut()

    val span = (high - low).toFloat()

    return IntArray(LUMINANCE_LEVELS) { level ->
        ((level - low) / span * MAX_LEVEL).roundToInt().coerceIn(0, MAX_LEVEL)
    }
}

private fun levelReaching(
    histogram: IntArray,
    target: Long,
): Int {
    var accumulated = 0L
    for (level in 0 until LUMINANCE_LEVELS) {
        accumulated += histogram[level]
        if (accumulated >= target) return level
    }

    return MAX_LEVEL
}

private fun identityLut(): IntArray = IntArray(LUMINANCE_LEVELS) { it }
