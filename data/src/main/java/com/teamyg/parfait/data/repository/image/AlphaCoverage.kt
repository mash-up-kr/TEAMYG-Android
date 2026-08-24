package com.teamyg.parfait.data.repository.image

/**
 * 후보의 "실제 크기"를 재는 지표. 불투명 픽셀 개수가 아니라 알파 총합인 것은, 개수로 세면 소프트
 * 매트 피사체(머리카락·유리)가 과소 계수되고 전 구간이 반투명인 피사체는 0으로 삭제되기 때문이다.
 *
 * `Long` 인 이유: 12MP 전면 불투명 후보의 합이 `Int` 를 넘어 음수로 래핑된다.
 */
internal fun sumAlpha(pixels: IntArray): Long {
    var sum = 0L
    for (pixel in pixels) sum += (pixel ushr 24).toLong()
    return sum
}
