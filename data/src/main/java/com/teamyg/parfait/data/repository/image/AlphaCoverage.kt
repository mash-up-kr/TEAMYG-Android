package com.teamyg.parfait.data.repository.image

/**
 * 후보의 "실제 크기"를 재는 지표.
 * specs/2026-08-24-segmentation-mask-postprocessing.md 「필터 판정」 참고.
 *
 * `Long` 인 이유: 12MP 전면 불투명 후보의 합이 `Int` 를 넘어 음수로 래핑된다.
 */
internal fun sumAlpha(pixels: IntArray): Long {
    var sum = 0L
    for (pixel in pixels) sum += (pixel ushr 24).toLong()
    return sum
}
