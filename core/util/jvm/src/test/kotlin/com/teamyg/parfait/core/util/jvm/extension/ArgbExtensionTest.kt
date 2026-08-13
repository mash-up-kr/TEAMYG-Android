package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertEquals

private const val OPAQUE_RED = 0xFFFF0000.toInt()
private const val OPAQUE_BLUE = 0xFF0000FF.toInt()

class ArgbExtensionTest {
    @Test
    fun fadeArgb_fullRatio_keepsColor() {
        // Given 불투명한 색
        val color = OPAQUE_RED

        // When 알파를 그대로 남기면
        val result = color.fadeArgb(1f)

        // Then 색이 그대로다
        assertEquals(OPAQUE_RED, result)
    }

    @Test
    fun fadeArgb_zeroRatio_keepsRgbButClearsAlpha() {
        val result = OPAQUE_RED.fadeArgb(0f)

        // 알파만 지우고 RGB 는 남겨야 가장자리를 흐려도 색이 검게 죽지 않는다
        assertEquals(0x00FF0000, result)
    }

    @Test
    fun fadeArgb_halfRatio_halvesAlpha() {
        val result = OPAQUE_RED.fadeArgb(0.5f)

        assertEquals(0x80FF0000.toInt(), result)
    }

    @Test
    fun fadeArgb_alreadyFadedColor_scalesFromItsOwnAlpha() {
        // Given 반쯤 투명한 색
        val color = 0x80FF0000.toInt()

        // When 다시 절반만 남기면
        val result = color.fadeArgb(0.5f)

        // Then 원래 알파에서 다시 절반이다
        assertEquals(0x40FF0000, result)
    }

    @Test
    fun mixArgb_fullRatio_returnsReceiver() {
        assertEquals(OPAQUE_RED, OPAQUE_RED.mixArgb(OPAQUE_BLUE, 1f))
    }

    @Test
    fun mixArgb_zeroRatio_returnsOther() {
        assertEquals(OPAQUE_BLUE, OPAQUE_RED.mixArgb(OPAQUE_BLUE, 0f))
    }

    @Test
    fun mixArgb_halfRatio_mixesEveryChannel() {
        // Given 알파까지 서로 다른 두 색
        val color = 0xFF804020.toInt()
        val other = 0x00402010

        // When 반씩 섞으면
        val result = color.mixArgb(other, 0.5f)

        // Then 네 채널이 각각 중간값이다
        assertEquals(0x80603018.toInt(), result)
    }

    @Test
    fun mixArgb_transparentOther_keepsRgbOfReceiver() {
        // 안쪽 겹과 바깥 겹을 이을 때 알파만 달라도 색이 어긋나지 않아야 한다
        val result = OPAQUE_RED.mixArgb(0x00FF0000, 0.5f)

        assertEquals(0x80FF0000.toInt(), result)
    }
}
