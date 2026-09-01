package com.teamyg.parfait.feature.segmentation.impl.component

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 테두리 미리보기가 그림을 놓는 자리.
 *
 * @param canvasWidth 뷰가 아니라 알맹이에 사방 여백을 더한 판의 크기다. 테두리도 이 판 위에 그려진다
 */
internal data class ToppingBorderPreviewLayout(
    val scale: Float,
    val subjectWidth: Int,
    val subjectHeight: Int,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val offsetX: Int,
    val offsetY: Int,
)

/**
 * 여백을 뺀 자리에 비트맵을 `Fit` 으로 맞추고, 여백까지 두른 판을 뷰 가운데에 놓는다.
 *
 * 테두리는 알맹이를 얹은 판 위에만 그려지므로 꽉 채우면 번져 나갈 자리가 없다. 여백은 그 자리다.
 *
 * @return 뷰가 아직 측정되지 않았거나 여백 둘을 빼면 놓을 자리가 남지 않으면 `null`
 */
internal fun toppingBorderPreviewLayoutOrNull(
    viewWidth: Int,
    viewHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    paddingPx: Int,
): ToppingBorderPreviewLayout? {
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return null

    val availableWidth = viewWidth - paddingPx * 2
    val availableHeight = viewHeight - paddingPx * 2
    if (availableWidth <= 0 || availableHeight <= 0) return null

    val scale = min(availableWidth.toFloat() / bitmapWidth, availableHeight.toFloat() / bitmapHeight)
    val subjectWidth = (bitmapWidth * scale).roundToInt()
    val subjectHeight = (bitmapHeight * scale).roundToInt()
    if (subjectWidth <= 0 || subjectHeight <= 0) return null

    val canvasWidth = subjectWidth + paddingPx * 2
    val canvasHeight = subjectHeight + paddingPx * 2

    return ToppingBorderPreviewLayout(
        scale = scale,
        subjectWidth = subjectWidth,
        subjectHeight = subjectHeight,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        offsetX = (viewWidth - canvasWidth) / 2,
        offsetY = (viewHeight - canvasHeight) / 2,
    )
}
