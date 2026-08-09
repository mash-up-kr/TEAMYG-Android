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
         * 비트맵을 비율을 지킨 채 [viewSize] 안에 잘리지 않게 넣고, 남는 여백을 양쪽에 똑같이 나눈다.
         * ImageView 의 FIT_CENTER 와 같은 배치이며, [zoom] 이 1 이면 가로/세로 모두 가운데 정렬이다.
         *
         * @param zoom 위 배치를 기준 1배로 삼는 확대 배율
         * @param pan 확대된 상태에서의 이동량. 이미지가 뷰포트 밖으로 빠지지 않도록 가둔 뒤 반영한다
         */
        fun fitCenter(
            viewSize: Size,
            bitmapWidth: Int,
            bitmapHeight: Int,
            zoom: Float = 1f,
            pan: Offset = Offset.Zero,
        ): BitmapViewMapping {
            val scale = fitScale(viewSize, bitmapWidth, bitmapHeight) * zoom
            val displayedWidth = bitmapWidth * scale
            val displayedHeight = bitmapHeight * scale
            val clampedPan = clampPan(pan, viewSize, bitmapWidth, bitmapHeight, zoom)
            return BitmapViewMapping(
                scale = scale,
                offsetX = (viewSize.width - displayedWidth) / 2f + clampedPan.x,
                offsetY = (viewSize.height - displayedHeight) / 2f + clampedPan.y,
            )
        }

        /** 뷰 크기가 [IntSize] 로 오는 포인터 입력 쪽을 위한 오버로드 */
        fun fitCenter(
            viewSize: IntSize,
            bitmapWidth: Int,
            bitmapHeight: Int,
            zoom: Float = 1f,
            pan: Offset = Offset.Zero,
        ): BitmapViewMapping = fitCenter(
            viewSize = Size(viewSize.width.toFloat(), viewSize.height.toFloat()),
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight,
            zoom = zoom,
            pan = pan,
        )
    }
}

/** 비율을 지킨 채 [viewSize] 안에 잘리지 않고 꽉 차게 담기는 배율 */
internal fun fitScale(
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Float = minOf(
    viewSize.width / bitmapWidth.toFloat(),
    viewSize.height / bitmapHeight.toFloat(),
)

/**
 * 확대된 이미지가 뷰포트 밖으로 빠지지 않도록 이동량을 가둔다.
 * 이미지가 뷰포트보다 작은 축은 이동할 여지가 없어 늘 가운데에 머문다.
 */
internal fun clampPan(
    pan: Offset,
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
    zoom: Float,
): Offset {
    val scale = fitScale(viewSize, bitmapWidth, bitmapHeight) * zoom
    val maxX = ((bitmapWidth * scale - viewSize.width) / 2f).coerceAtLeast(0f)
    val maxY = ((bitmapHeight * scale - viewSize.height) / 2f).coerceAtLeast(0f)
    return Offset(
        x = pan.x.coerceIn(-maxX, maxX),
        y = pan.y.coerceIn(-maxY, maxY),
    )
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
