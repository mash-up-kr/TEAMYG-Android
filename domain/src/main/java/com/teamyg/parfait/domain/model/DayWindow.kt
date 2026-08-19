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
        /**
         * 경계 시각(3시)만 [parfaitToday] 와 공유한다 — 시간대는 공유하지 않는다. 이 값은
         * [current] 에서 기기 시간대([TimeZone.currentSystemDefault])에, [parfaitToday] 에서는
         * 고정된 [PARFAIT_TIME_ZONE] 에 각각 물려 쓰인다.
         */
        const val DAY_BOUNDARY_HOUR = 3

        fun current(
            timeZone: TimeZone = TimeZone.currentSystemDefault(),
            clock: Clock = Clock.System,
        ): DayWindow {
            val now: LocalDateTime = clock.now().toLocalDateTime(timeZone)
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
