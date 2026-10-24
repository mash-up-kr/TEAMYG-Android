package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 타입 3종. 각 타입이 "네모배경 있음([hasFill])·바깥 테두리 있음([hasBorder])"을 스스로 안다 —
 * 렌더([TextToppingBackground])는 타입 이름이 아니라 이 플래그를 본다. `[타입]` 탭마다
 * 기본→배경→배경+테두리 순으로 순환하고, 새 텍스트는 [DEFAULT] 에서 시작한다.
 */
enum class TextToppingType(
    val hasFill: Boolean,
    val hasBorder: Boolean,
) {
    DEFAULT(hasFill = false, hasBorder = false),
    FILL(hasFill = true, hasBorder = false),
    FILL_STROKE(hasFill = true, hasBorder = true),
    ;

    fun next(): TextToppingType = entries[(ordinal + 1) % entries.size]
}
