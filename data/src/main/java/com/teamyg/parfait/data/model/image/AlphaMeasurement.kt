package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/**
 * @param partialAlphaPixels 알파가 1~254 인 픽셀 수
 */
internal data class AlphaMeasurement(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    val partialAlphaPixels: Int,
)
