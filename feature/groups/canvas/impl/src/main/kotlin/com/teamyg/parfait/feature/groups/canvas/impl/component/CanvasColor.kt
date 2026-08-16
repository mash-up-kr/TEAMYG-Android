package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.ui.graphics.Color

private const val HEX_RADIX = 16

private const val HEX_LENGTH_RGB = 6

private const val HEX_LENGTH_ARGB = 8

private const val OPAQUE_ALPHA = 0xFF000000L

/**
 * 캔버스 배경색·토핑 테두리색을 읽는다.
 *
 * 서버 계약이 두 값을 String 이라고만 정하고 형식을 말하지 않는다. `#RGB` 6·8 자리만 읽고
 * 나머지는 null 이다 — 여기서 형식을 넓히면 서버가 실제로 무엇을 주는지 확인하지 않은 채
 * 규칙이 굳는다. 못 읽은 색은 그리지 않는 쪽으로 떨어뜨린다.
 */
internal fun String.toCanvasColorOrNull(): Color? {
    val hex = removePrefix("#")
    if (hex.length != HEX_LENGTH_RGB && hex.length != HEX_LENGTH_ARGB) return null

    val argb = hex.toLongOrNull(HEX_RADIX) ?: return null

    return Color(if (hex.length == HEX_LENGTH_RGB) argb or OPAQUE_ALPHA else argb)
}
