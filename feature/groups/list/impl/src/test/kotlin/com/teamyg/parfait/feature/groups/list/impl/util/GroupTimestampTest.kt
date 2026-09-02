package com.teamyg.parfait.feature.groups.list.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class GroupTimestampTest {
    private val uploadedAt = Instant.parse("2026-08-15T10:00:00Z")

    private fun timestampAfter(elapsed: Duration) = uploadedAt.toGroupTimestamp(now = uploadedAt + elapsed)

    @Test
    fun nullUploadedAt_isNoImage() {
        // Given 아직 아무도 토핑을 올리지 않은 그룹
        // When 경과 단위를 고른다
        val timestamp = null.toGroupTimestamp(now = uploadedAt)

        // Then 조회 실패가 아니라 "이미지 없음" 갈래로 떨어진다
        assertEquals(GroupTimestamp.NoImage, timestamp)
    }

    @Test
    fun sameInstantWrittenInAnotherOffset_isTheSameElapsed() {
        // Given 같은 순간을 UTC 와 KST 표기로 각각 받는다
        val utc = Instant.parse("2026-08-15T05:17:10Z")
        val kst = Instant.parse("2026-08-15T14:17:10+09:00")
        val now = utc + 30.minutes

        // When 각각 경과 단위를 고른다
        // Then 표기가 달라도 같은 순간이라 같은 값이 나온다
        assertEquals(utc.toGroupTimestamp(now), kst.toGroupTimestamp(now))
        assertEquals(GroupTimestamp.Minutes(30), kst.toGroupTimestamp(now))
    }

    @Test
    fun underOneMinute_isJustNow() {
        assertEquals(GroupTimestamp.JustNow, timestampAfter(59.seconds))
    }

    @Test
    fun oneMinute_isMinutes() {
        assertEquals(GroupTimestamp.Minutes(1), timestampAfter(1.minutes))
    }

    @Test
    fun underOneHour_isMinutes() {
        assertEquals(GroupTimestamp.Minutes(59), timestampAfter(59.minutes + 59.seconds))
    }

    @Test
    fun oneHour_isHours() {
        assertEquals(GroupTimestamp.Hours(1), timestampAfter(1.hours))
    }

    @Test
    fun underOneDay_isHours() {
        assertEquals(GroupTimestamp.Hours(23), timestampAfter(23.hours + 59.minutes))
    }

    @Test
    fun oneDay_isDays() {
        assertEquals(GroupTimestamp.Days(1), timestampAfter(1.days))
    }

    @Test
    fun futureUploadedAt_isJustNow() {
        // Given 기기 시계가 서버보다 뒤처져 업로드 시각이 미래로 들어온다
        // When 경과 단위를 고른다
        val timestamp = timestampAfter(-5.minutes)

        // Then 음수 경과를 노출하지 않고 방금 전으로 본다
        assertEquals(GroupTimestamp.JustNow, timestamp)
    }
}
