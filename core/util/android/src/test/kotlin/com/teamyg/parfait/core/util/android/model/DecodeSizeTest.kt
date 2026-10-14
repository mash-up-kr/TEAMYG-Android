package com.teamyg.parfait.core.util.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecodeSizeTest {
    @Test
    fun capped_crashReportedSize_fitsUnderDrawLimit() {
        // Given Firebase 에 잡힌 치수 — ARGB_8888 로 170,943,360 bytes 다
        val capped = DecodeSize.capped(width = 5520, height = 7742)

        // Then RecordingCanvas 가 거부하는 100MB 아래로 내려온다
        assertEquals(DecodeSize.LONG_SIDE_LIMIT, capped.height)
        assertTrue(capped.width.toLong() * capped.height * 4 < 100L * 1024 * 1024)
    }

    @Test
    fun capped_overLimit_keepsAspectRatio() {
        val capped = DecodeSize.capped(width = 8192, height = 6144)

        assertEquals(DecodeSize(4096, 3072), capped)
    }

    @Test
    fun capped_withinLimit_isUnchanged() {
        // Given 기기 기본 촬영 해상도와 상한 경계
        assertEquals(DecodeSize(4000, 3000), DecodeSize.capped(width = 4000, height = 3000))
        assertEquals(DecodeSize(4096, 4096), DecodeSize.capped(width = 4096, height = 4096))
    }

    @Test
    fun capped_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 반올림하면 짧은 변이 0 이 되는 띠 모양 — 0 은 디코더가 던진다
        val capped = DecodeSize.capped(width = 40960, height = 1)

        assertEquals(DecodeSize(4096, 1), capped)
    }
}
