package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringTest {
    @Test
    fun toColorOrNull_sixDigitsWithHash_readsAsOpaqueColor() {
        // Given 서버가 주는 형식
        val hex = "#FF6B00"

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

    @Test
    fun toArgbHexString_opaqueColor_writesEightDigitsWithHash() {
        // Given 초안이 담는 ARGB Int
        val argb = Color(0xFFFF6B00).toArgb()

        // Then 서버에 보내는 형식은 8자리다 — 6자리로 보내면 알파가 사라진다
        assertEquals("#FFFF6B00", argb.toArgbHexString())
    }

    @Test
    fun toArgbHexString_thenToColorOrNull_roundTrips() {
        // 이 왕복이 깨지면 캔버스가 테두리를 조용히 안 그린다 — 서버는 200 을 준다
        val original = Color(0x80123456)
        val restored = original.toArgb().toArgbHexString().toColorOrNull()

        assertEquals(original, restored)
    }

    @Test
    fun toArgbHexString_transparentBlack_keepsLeadingZeros() {
        // 앞자리 0 이 잘리면 길이가 8 이 아니게 되고 읽기 쪽이 null 을 낸다
        assertEquals("#00000000", 0.toArgbHexString())
    }
}
