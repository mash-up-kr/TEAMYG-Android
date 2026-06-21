package com.teamyg.parfait.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * 하루 경계가 새벽 3시인 윈도우
 * e.g. 6일 03:00 ~ 7일 02:59
 */
data class DayWindow(
    val startMs: Long,
    val endMs: Long,
) {
    operator fun contains(timestampMs: Long): Boolean = timestampMs in startMs..<endMs

    companion object {
        const val DAY_BOUNDARY_HOUR = 3

        fun current(timeZone: TimeZone = TimeZone.currentSystemDefault()): DayWindow {
            val now: LocalDateTime = Clock.System.now().toLocalDateTime(timeZone)
            val anchorDate: LocalDate = when (now.time >= LocalTime(DAY_BOUNDARY_HOUR, 0)) {
                true -> now.date
                false -> now.date.minus(1, DateTimeUnit.DAY)
            }

            val startInstant: Instant = anchorDate
                .atStartOfDayIn(timeZone)
                .plus(DAY_BOUNDARY_HOUR.hours)
            val endInstant: Instant = startInstant.plus(24.hours)

            return DayWindow(
                startMs = startInstant.toEpochMilliseconds(),
                endMs = endInstant.toEpochMilliseconds(),
            )
        }
    }
}
