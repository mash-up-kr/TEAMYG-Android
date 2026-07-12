package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal data class BitmapViewMapping(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal fun bitmapToViewMapping(
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): BitmapViewMapping {
    val scale = minOf(
        viewSize.width / bitmapWidth.toFloat(),
        viewSize.height / bitmapHeight.toFloat(),
    )
    val displayedW = bitmapWidth * scale
    val displayedH = bitmapHeight * scale
    val offsetX = (viewSize.width - displayedW) / 2f
    val offsetY = (viewSize.height - displayedH) / 2f
    return BitmapViewMapping(scale, offsetX, offsetY)
}

internal fun mapViewToBitmap(
    point: Offset,
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Pair<Int, Int>? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null
    val mapping = bitmapToViewMapping(viewSize, bitmapWidth, bitmapHeight)
    val x = ((point.x - mapping.offsetX) / mapping.scale).toInt()
    val y = ((point.y - mapping.offsetY) / mapping.scale).toInt()
    if (x !in 0 until bitmapWidth || y !in 0 until bitmapHeight) return null
    return x to y
}

internal fun mapViewToBitmapFloat(
    point: Offset,
    mapping: BitmapViewMapping,
): Offset = Offset(
    x = (point.x - mapping.offsetX) / mapping.scale,
    y = (point.y - mapping.offsetY) / mapping.scale,
)
