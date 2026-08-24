package com.teamyg.parfait.data.repository.image

internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int = (value + divisor - 1) / divisor

/**
 * 알파를 [threshold] 로 이진화하고 [factor] × [factor] 블록마다 OR 해서 축소 마스크를 만든다.
 *
 * 이진화를 먼저 하고 OR 하는 것과 최댓값을 먼저 구하고 이진화하는 것은 결과가 같지만
 * (`max(aᵢ) > t ⇔ ∃i: aᵢ > t`), 이 순서면 축소 버퍼가 알파 바이트가 아니라 참·거짓이면 된다.
 * 평균으로 줄이면 그 동치가 깨지고 1~2픽셀 폭 구조가 축소에서 끊긴다.
 *
 * 치수가 [factor] 의 배수가 아닐 때 올림하는 이유: 내림하면 오른쪽·아래 가장자리가 판정에서
 * 빠져 그 자리 알파가 0이 되고, 프레임에 걸친 피사체의 테두리가 사라진다.
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
