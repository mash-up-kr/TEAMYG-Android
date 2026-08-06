package com.teamyg.parfait.domain.model

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DayWindowTest {
    private val seoul = TimeZone.of("Asia/Seoul")

    private fun fixedClock(iso: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(iso)
    }

    @Test
    fun current_justAfterBoundary_anchorsToSameDay() {
        // Given 한국 시간 8월 6일 03:00 정각 (UTC 로는 8월 5일 18:00)
        val clock = fixedClock("2026-08-05T18:00:00Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 윈도우 시작은 8월 6일 03:00 이다
        assertEquals(
            Instant.parse("2026-08-05T18:00:00Z").toEpochMilliseconds(),
            window.startMs,
        )
    }

    @Test
    fun current_justBeforeBoundary_anchorsToPreviousDay() {
        // Given 한국 시간 8월 6일 02:59:59 (UTC 로는 8월 5일 17:59:59)
        val clock = fixedClock("2026-08-05T17:59:59Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 윈도우 시작은 하루 전인 8월 5일 03:00 이다
        assertEquals(
            Instant.parse("2026-08-04T18:00:00Z").toEpochMilliseconds(),
            window.startMs,
        )
    }

    @Test
    fun current_anyMoment_windowSpans24Hours() {
        // Given 임의 시각
        val clock = fixedClock("2026-08-05T23:30:00Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 길이는 정확히 24시간이다
        assertEquals(24L * 60 * 60 * 1000, window.endMs - window.startMs)
    }

    @Test
    fun contains_startBoundary_returnsTrue() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertTrue(3_000L in window)
    }

    @Test
    fun contains_endBoundary_returnsFalse() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertFalse(5_000L in window)
    }

    @Test
    fun contains_beforeStart_returnsFalse() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertFalse(2_999L in window)
    }

    @Test
    fun contains_insideRange_returnsTrue() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertTrue(4_999L in window)
    }
}
