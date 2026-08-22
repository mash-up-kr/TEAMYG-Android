package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {
    @Test
    fun toRgbHex_opaqueColor_writesSixDigitsWithHash() {
        // Given, When
        val hex = Color(0xFFFF6B00).toRgbHex()

        // Then 서버가 받는 형식은 `#` + 6자리다
        assertEquals("#FF6B00", hex)
    }

    @Test
    fun toRgbHex_translucentColor_dropsTheAlpha() {
        // Given, When 알파가 섞인 색
        val hex = Color(0x80FF6B00).toRgbHex()

        // Then 알파 자리는 빠지고 RGB 만 남는다 — 8자리는 서버가 거부한다
        assertEquals("#FF6B00", hex)
    }

    @Test
    fun toRgbHex_roundTrips_throughToColorOrNull() {
        // Given 팔레트에 있을 법한 색
        val color = Color(0xFFC2E4FC)

        // When 적었다가 다시 읽는다
        val restored = color.toRgbHex().toColorOrNull()

        // Then 같은 색이다
        assertEquals(color, restored)
    }
}
