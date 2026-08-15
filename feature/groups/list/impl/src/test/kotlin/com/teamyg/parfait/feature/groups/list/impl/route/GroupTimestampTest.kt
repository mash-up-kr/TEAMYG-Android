package com.teamyg.parfait.feature.groups.list.impl.route

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class GroupTimestampTest {
    private val timeZone = TimeZone.UTC
    private val uploadedAt = LocalDateTime(2026, 8, 15, 10, 0)
    private val uploadedInstant = uploadedAt.toInstant(timeZone)

    private fun timestampAfter(elapsed: kotlin.time.Duration) =
        uploadedAt.toGroupTimestamp(now = uploadedInstant + elapsed, timeZone = timeZone)

    @Test
    fun nullUploadedAt_hasNoTimestamp() {
        // Given 이미지가 한 장도 없는 그룹
        // When 경과 단위를 고른다
        val timestamp = null.toGroupTimestamp(now = uploadedInstant, timeZone = timeZone)

        // Then 잴 기준 시각이 없어 표시할 것도 없다
        assertNull(timestamp)
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
