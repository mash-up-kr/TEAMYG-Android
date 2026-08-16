package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.ui.graphics.Color

private const val HEX_RADIX = 16

private const val HEX_LENGTH_RGB = 6

private const val HEX_LENGTH_ARGB = 8

private const val OPAQUE_ALPHA = 0xFF000000L

/**
 * 캔버스 배경색·토핑 테두리색을 읽는다.
 *
 * 서버가 `#RRGGBB` 여섯 자리로 준다. 계약에는 String 이라고만 적혀 있어 형식이 바뀌어도
 * 컴파일로는 드러나지 않으므로, 알파가 붙은 여덟 자리도 함께 읽어 둔다 — 그때 테두리가
 * 통째로 사라지는 것보다 낫다.
 *
 * 그 밖의 형식은 null 이다. 못 읽은 색은 그리지 않는 쪽으로 떨어뜨린다.
 */
internal fun String.toCanvasColorOrNull(): Color? {
    val hex = removePrefix("#")
    if (hex.length != HEX_LENGTH_RGB && hex.length != HEX_LENGTH_ARGB) return null

    val argb = hex.toLongOrNull(HEX_RADIX) ?: return null

    return Color(if (hex.length == HEX_LENGTH_RGB) argb or OPAQUE_ALPHA else argb)
}
