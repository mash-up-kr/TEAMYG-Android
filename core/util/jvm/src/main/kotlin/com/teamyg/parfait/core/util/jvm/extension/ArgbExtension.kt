package com.teamyg.parfait.core.util.jvm.extension

import kotlin.math.roundToInt

/*
 * 픽셀을 하나씩 훑어 칠할 때 쓰는 색 연산.
 *
 * 화면에 쓸 색은 Compose Color 로 다루는 것이 맞다. 여기 있는 것은 픽셀 배열을 직접 채우느라
 * 색 하나마다 변환을 태울 수 없는 자리를 위해, ARGB 정수를 그대로 만지는 연산이다.
 */

private const val CHANNEL_SHIFT_ALPHA = 24

private const val CHANNEL_MASK = 0xFF

private const val RGB_MASK = 0x00FFFFFF

private val ARGB_CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)

/** 색은 두고 알파만 [ratio] 만큼 남긴다 */
fun Int.fadeArgb(ratio: Float): Int {
    val alpha = ((this ushr CHANNEL_SHIFT_ALPHA and CHANNEL_MASK) * ratio).roundToInt().coerceIn(0, CHANNEL_MASK)
    return (alpha shl CHANNEL_SHIFT_ALPHA) or (this and RGB_MASK)
}

/** 네 채널을 각각 섞는다. [ratio] 가 1 이면 이 색이고 0 이면 [other] 다 */
fun Int.mixArgb(
    other: Int,
    ratio: Float,
): Int {
    var mixed = 0
    for (shift in ARGB_CHANNEL_SHIFTS) {
        val channel = (this ushr shift and CHANNEL_MASK) * ratio + (other ushr shift and CHANNEL_MASK) * (1f - ratio)
        mixed = mixed or (channel.roundToInt().coerceIn(0, CHANNEL_MASK) shl shift)
    }
    return mixed
}
