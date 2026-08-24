package com.teamyg.parfait.data.repository.image

internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int = (value + divisor - 1) / divisor

/**
 * 알파를 [threshold] 로 이진화하고 [factor] × [factor] 블록마다 OR 해서 축소 마스크를 만든다.
 *
 * See: `specs/2026-08-24-segmentation-mask-postprocessing.md` 「처리 해상도」
 */
internal fun downscaleMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    factor: Int,
    threshold: Int,
    checkCancelled: () -> Unit = {},
): BooleanArray {
    val maskWidth = ceilDiv(width, factor)
    val mask = BooleanArray(maskWidth * ceilDiv(height, factor))

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            if ((alpha[rowOffset + x].toInt() and 0xFF) > threshold) {
                mask[maskRowOffset + x / factor] = true
            }
        }
    }

    return mask
}
