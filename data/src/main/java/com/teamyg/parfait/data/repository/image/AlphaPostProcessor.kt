package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds

private const val OPAQUE = 255

/**
 * [keep] 이 거짓인 자리의 알파를 0으로 만든다. 참인 자리는 **원본 알파를 그대로 둔다.**
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun applyKeepMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    keep: BooleanArray,
    maskWidth: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            val index = rowOffset + x
            if ((alpha[index].toInt() and 0xFF) == 0) continue
            if (keep[maskRowOffset + x / factor]) continue

            alpha[index] = 0
            changed = true
        }
    }

    return changed
}

internal data class AlphaMeasurement(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    /** 알파가 1~254 인 픽셀 수. 관측이 램프 띠 폭과 침식 유효성을 판정하는 데 쓴다 */
    val partialAlphaPixels: Int,
)

/**
 * 남은 알파를 감싸는 사각 영역과 커버리지를 잰다.
 *
 * 불투명 판정을 "알파 0 초과"로 두는 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「tight bounds 판정 기준 통일」에 있다.
 *
 * @return 남은 알파가 없으면 `null`
 */
internal fun measureAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): AlphaMeasurement? {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    var sum = 0L
    var partial = 0

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        for (x in 0 until width) {
            val value = alpha[rowOffset + x].toInt() and 0xFF
            if (value == 0) continue

            sum += value
            if (value < OPAQUE) partial++
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    if (right < 0) return null

    return AlphaMeasurement(
        // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
        bounds = SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1),
        alphaSum = sum,
        partialAlphaPixels = partial,
    )
}
