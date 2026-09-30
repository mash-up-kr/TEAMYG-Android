package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds

internal fun SegmentationBounds.offsetBy(
    dx: Int,
    dy: Int,
): SegmentationBounds = SegmentationBounds(
    left = left + dx,
    top = top + dy,
    right = right + dx,
    bottom = bottom + dy,
)

/** 어기면 예외 없이 저장할 때 조용히 잘린다 */
internal fun isInsideCanvas(
    bounds: SegmentationBounds,
    canvasWidth: Int,
    canvasHeight: Int,
): Boolean = bounds.left >= 0 &&
    bounds.top >= 0 &&
    bounds.right <= canvasWidth &&
    bounds.bottom <= canvasHeight
