package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize

internal data class BitmapViewMapping(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    companion object {
        /**
         * 비트맵을 비율을 지킨 채 [viewSize] 안에 넣고, 남는 여백을 양쪽에 똑같이 나눈다.
         * ImageView 의 FIT_CENTER 와 같은 배치다.
         */
        fun fitCenter(
            viewSize: Size,
            bitmapWidth: Int,
            bitmapHeight: Int,
        ): BitmapViewMapping {
            val scale = minOf(
                viewSize.width / bitmapWidth.toFloat(),
                viewSize.height / bitmapHeight.toFloat(),
            )
            val displayedWidth = bitmapWidth * scale
            val displayedHeight = bitmapHeight * scale
            return BitmapViewMapping(
                scale = scale,
                offsetX = (viewSize.width - displayedWidth) / 2f,
                offsetY = (viewSize.height - displayedHeight) / 2f,
            )
        }

        /** 뷰 크기가 [IntSize] 로 오는 포인터 입력 쪽을 위한 오버로드 */
        fun fitCenter(
            viewSize: IntSize,
            bitmapWidth: Int,
            bitmapHeight: Int,
        ): BitmapViewMapping = fitCenter(
            viewSize = Size(viewSize.width.toFloat(), viewSize.height.toFloat()),
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
        )
    }
}

internal fun mapViewToBitmap(
    point: Offset,
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Pair<Int, Int>? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null
    val mapping = BitmapViewMapping.fitCenter(viewSize, bitmapWidth, bitmapHeight)
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

internal fun mapBitmapToViewFloat(
    point: Offset,
    mapping: BitmapViewMapping,
): Offset = Offset(
    x = point.x * mapping.scale + mapping.offsetX,
    y = point.y * mapping.scale + mapping.offsetY,
)
