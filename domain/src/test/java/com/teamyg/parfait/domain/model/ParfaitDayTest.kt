package com.teamyg.parfait.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class ParfaitDayTest {
    private fun fixedClock(iso: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(iso)
    }

    @Test
    fun parfaitToday_justBeforeBoundary_isStillYesterday() {
        // Given 한국 시간 8월 18일 02:59 (UTC 로는 8월 17일 17:59)
        val clock = fixedClock("2026-08-17T17:59:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 아직 전날 캔버스가 진행 중이다 — 서버 ParfaitDay 와 같은 기준
        assertEquals(LocalDate(2026, 8, 17), today)
    }

    @Test
    fun parfaitToday_atBoundary_rollsOver() {
        // Given 한국 시간 8월 18일 03:00 정각
        val clock = fixedClock("2026-08-17T18:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 경계 정각부터 새 날이다
        assertEquals(LocalDate(2026, 8, 18), today)
    }

    @Test
    fun parfaitToday_justAfterMidnight_isYesterday() {
        // Given 한국 시간 8월 18일 00:00 정각 (UTC 로는 8월 17일 15:00)
        val clock = fixedClock("2026-08-17T15:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 자정은 경계가 아니다 — 달력이 넘어가도 캔버스는 안 넘어간다
        assertEquals(LocalDate(2026, 8, 17), today)
    }

    @Test
    fun parfaitToday_lateEvening_isSameCalendarDay() {
        // Given 한국 시간 8월 18일 23:59 (UTC 로는 8월 18일 14:59)
        val clock = fixedClock("2026-08-18T14:59:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 경계 뒤라 달력 날짜와 같다
        assertEquals(LocalDate(2026, 8, 18), today)
    }

    @Test
    fun parfaitToday_usesSeoulNotDeviceZone() {
        // Given 한국 시간 8월 18일 11:00 인 순간 (UTC 로는 8월 18일 02:00)
        val clock = fixedClock("2026-08-18T02:00:00Z")

        // When 파르페 기준의 오늘을 센다
        val today = parfaitToday(clock)

        // Then 기기 시간대와 무관하게 KST 로 센다 — CI 가 UTC 여도 같은 답이 나온다
        assertEquals(LocalDate(2026, 8, 18), today)
    }
}
