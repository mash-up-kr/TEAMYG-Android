package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.graphics.Color

private const val HEX_RADIX = 16

private const val HEX_LENGTH_RGB = 6

private const val HEX_LENGTH_ARGB = 8

private const val OPAQUE_ALPHA = 0xFF000000L

/**
 * 16진수 문자열을 색으로 읽는다. `#` 은 있어도 없어도 된다.
 *
 * 서버가 주는 색은 `#RRGGBB` 여섯 자리다. 계약에는 String 이라고만 적혀 있어 형식이 바뀌어도
 * 컴파일로는 드러나지 않으므로, 알파가 붙은 여덟 자리도 함께 읽어 둔다 — 그때 색이 통째로
 * 사라지는 것보다 낫다.
 */
fun String.toColorOrNull(): Color? {
    val hex = removePrefix("#")
    if (hex.length != HEX_LENGTH_RGB && hex.length != HEX_LENGTH_ARGB) return null

    val argb = hex.toLongOrNull(HEX_RADIX) ?: return null

    return Color(if (hex.length == HEX_LENGTH_RGB) argb or OPAQUE_ALPHA else argb)
}
