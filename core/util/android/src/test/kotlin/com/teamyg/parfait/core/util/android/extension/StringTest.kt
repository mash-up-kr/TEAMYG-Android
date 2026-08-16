package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringTest {
    @Test
    fun toColorOrNull_sixDigitsWithHash_readsAsOpaqueColor() {
        // Given 서버가 주는 형식
        val hex = "#FF6B00"

        // When
        val color = hex.toColorOrNull()

        // Then 알파가 없으니 불투명으로 채운다
        assertEquals(Color(0xFFFF6B00), color)
    }

    @Test
    fun toColorOrNull_noHashPrefix_readsTheSame() {
        assertEquals("FF6B00".toColorOrNull(), "#FF6B00".toColorOrNull())
    }

    @Test
    fun toColorOrNull_eightDigits_keepsTheAlpha() {
        // 계약은 여섯 자리지만 알파가 붙어 와도 색이 통째로 사라지지 않아야 한다
        assertEquals(Color(0x80FF6B00), "#80FF6B00".toColorOrNull())
    }

    @Test
    fun toColorOrNull_unsupportedLength_returnsNull() {
        // 세 자리 축약형은 읽지 않는다 — 지원하는 척하면 엉뚱한 색이 나온다
        assertNull("#F60".toColorOrNull())
    }

    @Test
    fun toColorOrNull_notHex_returnsNull() {
        assertNull("#GGGGGG".toColorOrNull())
    }
}
