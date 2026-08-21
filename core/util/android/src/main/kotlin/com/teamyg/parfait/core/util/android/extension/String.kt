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

/**
 * ARGB 정수를 서버에 보내는 색 문자열로 쓴다. [toColorOrNull] 의 역함수(불투명 색만).
 *
 * 계약은 `#RRGGBB` 6자리다. 8자리로 쓰면 iOS·CSS 파서가 `#RRGGBBAA`로 읽어 이 함수가 쓰는
 * ARGB 관례와 어긋나는데, 서버는 그 문자열을 검증 없이 그대로 저장·반환해 어긋나도 드러나지
 * 않는다 — 형식이 어긋나면 읽기 쪽이 `null` 을 내 캔버스가 테두리를 그냥 안 그린다.
 *
 * 알파가 불투명이 아니면 6자리로 조용히 잘리는 대신 여기서 막는다 — 이 함수가 받는 색은
 * 팔레트에서 오는 불투명 색뿐이라 지금은 도달하지 않는 자리다.
 */
fun Int.toRgbHexString(): String {
    val alpha = (this ushr 24) and 0xFF
    require(alpha == 0xFF) { "불투명 색만 지원한다: alpha=$alpha" }
    return "#%06X".format(this and 0xFFFFFF)
}
