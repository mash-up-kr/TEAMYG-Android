package com.teamyg.parfait.feature.segmentation.impl.component

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.min

/**
 * 화면에 그려진 자리. Compose 타입을 쓰지 않는 것은 그리기와 탭 판정이 공유하는 이 계산을
 * 기기 없이 검증하기 위해서다.
 */
internal data class ScaledRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left

    val height: Float get() = bottom - top

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x in left..right && y in top..bottom
}

/**
 * 원본 픽셀 좌표인 [bounds] 를 `ContentScale.Fit` 으로 그려진 화면 좌표로 옮긴다.
 *
 * @return 이미지 치수가 아직 유효하지 않으면 `null`
 */
internal fun scaledRectOrNull(
    bounds: SegmentationBounds,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
): ScaledRect? {
    if (imageWidth <= 0 || imageHeight <= 0) return null

    val scale = min(canvasWidth / imageWidth, canvasHeight / imageHeight)
    val offsetX = (canvasWidth - imageWidth * scale) / 2f
    val offsetY = (canvasHeight - imageHeight * scale) / 2f

    return ScaledRect(
        left = offsetX + bounds.left * scale,
        top = offsetY + bounds.top * scale,
        right = offsetX + bounds.right * scale,
        bottom = offsetY + bounds.bottom * scale,
    )
}

/**
 * 탭한 자리에 걸리는 후보 중 **면적이 가장 작은 것**을 고른다.
 *
 * 큰 후보 안에 작은 후보가 들어 있을 때 바깥을 고르면 안쪽 대상을 영영 못 고른다. 목록이 면적
 * 내림차순이라 "뒤에서부터 첫 히트"로도 같은 결과가 나오지만, 그렇게 쓰면 이 함수의 올바름이
 * 호출부의 정렬 기준에 매달린다.
 *
 * @return 걸리는 후보가 없으면 `null`
 */
internal fun pickCandidateIndex(
    boundsList: List<SegmentationBounds>,
    imageWidth: Int,
    imageHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    tapX: Float,
    tapY: Float,
): Int? = boundsList
    .withIndex()
    .filter { (_, bounds) ->
        scaledRectOrNull(bounds, imageWidth, imageHeight, canvasWidth, canvasHeight)
            ?.contains(tapX, tapY) == true
    }.minByOrNull { (_, bounds) -> bounds.width.toLong() * bounds.height }
    ?.index
