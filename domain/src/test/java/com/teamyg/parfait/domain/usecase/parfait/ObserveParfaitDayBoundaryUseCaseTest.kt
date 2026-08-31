package com.teamyg.parfait.domain.usecase.parfait

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private fun atKst(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int = 0,
): Instant = LocalDateTime(year, month, day, hour, minute).toInstant(PARFAIT_TIME_ZONE)

private class FixedClock(
    private val instant: Instant,
) : Clock {
    override fun now(): Instant = instant
}

private class MutableClock(
    private var instant: Instant,
) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}

class ObserveParfaitDayBoundaryUseCaseTest {
    @Test
    fun invoke_emitsTheCurrentParfaitDayFirst() = runTest {
        val clock = FixedClock(atKst(2026, 8, 27, hour = 10))

        ObserveParfaitDayBoundaryUseCase().invoke(clock).test {
            assertEquals(LocalDate(2026, 8, 27), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_crossingTheBoundary_emitsTheNewDay() = runTest {
        val clock = MutableClock(atKst(2026, 8, 28, hour = 2, minute = 59))

        ObserveParfaitDayBoundaryUseCase().invoke(clock).test {
            // 새벽 3시 전이라 아직 27일이다
            assertEquals(LocalDate(2026, 8, 27), awaitItem())

            clock.advanceBy(2.minutes)
            advanceTimeBy(2.minutes)

            assertEquals(LocalDate(2026, 8, 28), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
