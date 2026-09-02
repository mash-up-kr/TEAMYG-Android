package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds

/** 알파가 0이면 완전 투명(RGB 까지 0)으로 접고, 그 외에는 [alpha] 를 얹은 채 원본 RGB 를 남긴다 */
private fun compositeAlpha(
    rgb: Int,
    alpha: Int,
): Int = if (alpha == 0) 0 else (alpha shl 24) or (rgb and 0x00FFFFFF)

/**
 * 원본에서 잘라 온 [pixels] 에 후처리한 알파를 얹는다. [alpha] 는 원본 전체 좌표계라
 * [bounds] 로 오프셋을 잡아 읽는다.
 */
internal fun applyAlphaInPlace(
    pixels: IntArray,
    alpha: ByteArray,
    alphaRowStride: Int,
    bounds: SegmentationBounds,
) {
    for (y in 0 until bounds.height) {
        val alphaRow = (bounds.top + y) * alphaRowStride + bounds.left
        val pixelRow = y * bounds.width
        for (x in 0 until bounds.width) {
            val value = alpha[alphaRow + x].toInt() and 0xFF
            pixels[pixelRow + x] = compositeAlpha(pixels[pixelRow + x], value)
        }
    }
}

/**
 * 출력 판을 bounds 크기로 바로 만든다. 원본 크기로 만들고 나중에 자르면 큰 배열이 헛돈다.
 *
 * ⚠️ [applyAlphaInPlace] 와 합치지 마라 — 소스 인덱싱이 다르다. 이쪽은 [pixels] 도 [alpha] 도
 * 같은 [rowStride] 를 쓰는 판 로컬 좌표계라 두 배열을 같은 오프셋으로 읽는다. [applyAlphaInPlace]
 * 는 [alpha] 만 원본 좌표계고 pixels 는 이미 bounds 크기로 잘려 있어 오프셋이 따로 든다. 합치면
 * 폴백 경로에 bounds 크기 배열이 한 벌 더 생긴다(12MP 면 48MB).
 */
internal fun composeCroppedArgb(
    pixels: IntArray,
    alpha: ByteArray,
    rowStride: Int,
    bounds: SegmentationBounds,
): IntArray {
    val cropped = IntArray(bounds.width * bounds.height)
    for (y in 0 until bounds.height) {
        val sourceRow = (bounds.top + y) * rowStride + bounds.left
        val targetRow = y * bounds.width
        for (x in 0 until bounds.width) {
            val value = alpha[sourceRow + x].toInt() and 0xFF
            cropped[targetRow + x] = compositeAlpha(pixels[sourceRow + x], value)
        }
    }
    return cropped
}
