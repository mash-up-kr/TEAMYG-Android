package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 저장하지 못한 토핑. 이미지와 텍스트는 id 가 겹칠 수 있어 집합을 나눈다 */
internal data class ToppingSaveFailures(
    val toppingIds: Set<Long> = emptySet(),
    val textIds: Set<Long> = emptySet(),
) {
    fun isEmpty(): Boolean = toppingIds.isEmpty() && textIds.isEmpty()
}
