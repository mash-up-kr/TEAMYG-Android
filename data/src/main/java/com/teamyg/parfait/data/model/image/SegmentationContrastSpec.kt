package com.teamyg.parfait.data.model.image

internal object SegmentationContrastSpec {
    const val LUMINANCE_LEVELS = 256

    const val LOW_PERCENTILE = 0.01f

    const val HIGH_PERCENTILE = 0.99f

    const val MAX_LEVEL = LUMINANCE_LEVELS - 1
}
