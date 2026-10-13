package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecodeSizeTest {
    @Test
    fun cappedDecodeSize_crashReportedSize_fitsUnderDrawLimit() {
        // Given Firebase 에 잡힌 치수 — ARGB_8888 로 170,943,360 bytes 다
        val capped = cappedDecodeSize(width = 5520, height = 7742)

        // Then RecordingCanvas 가 거부하는 100MB 아래로 내려온다
        assertEquals(DECODE_LONG_SIDE_LIMIT, capped.height)
        assertTrue(capped.width.toLong() * capped.height * 4 < 100L * 1024 * 1024)
    }

    @Test
    fun cappedDecodeSize_overLimit_keepsAspectRatio() {
        val capped = cappedDecodeSize(width = 8192, height = 6144)

        assertEquals(IntSize(4096, 3072), capped)
    }

    @Test
    fun cappedDecodeSize_withinLimit_isUnchanged() {
        // Given 기기 기본 촬영 해상도와 상한 경계
        assertEquals(IntSize(4000, 3000), cappedDecodeSize(width = 4000, height = 3000))
        assertEquals(IntSize(4096, 4096), cappedDecodeSize(width = 4096, height = 4096))
    }

    @Test
    fun cappedDecodeSize_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 반올림하면 짧은 변이 0 이 되는 띠 모양 — 0 은 디코더가 던진다
        val capped = cappedDecodeSize(width = 40960, height = 1)

        assertEquals(IntSize(4096, 1), capped)
    }
}
