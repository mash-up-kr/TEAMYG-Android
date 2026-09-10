package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.roundToInt

/** 축마다 배율이 다른 것은 목표 치수를 정수로 반올림하기 때문이다 */
internal data class RecoveryTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Int,
    val offsetY: Int,
) {
    fun toOrigin(bounds: DetectionBounds): SegmentationBounds = SegmentationBounds(
        left = offsetX + (bounds.left * scaleX).roundToInt(),
        top = offsetY + (bounds.top * scaleY).roundToInt(),
        right = offsetX + (bounds.right * scaleX).roundToInt(),
        bottom = offsetY + (bounds.bottom * scaleY).roundToInt(),
    )
}
