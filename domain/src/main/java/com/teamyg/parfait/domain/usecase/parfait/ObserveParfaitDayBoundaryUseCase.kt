package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.DayWindow
import com.teamyg.parfait.domain.model.PARFAIT_TIME_ZONE
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * 파르페 기준의 오늘을 내고, 하루 경계(새벽 3시)를 넘길 때마다 새 날짜를 다시 낸다.
 *
 * 값 스트림에 필터를 다는 방식으로는 이 판정을 못 한다 — 캔버스 캐시가 조용하면 재방출이 없어
 * 필터가 아예 평가되지 않는다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
 */
class ObserveParfaitDayBoundaryUseCase
@Inject
constructor() {
    /** @param clock 경계 판정과 대기 시간 계산에 쓴다. 테스트에서 경계 앞뒤를 고정한다 */
    operator fun invoke(clock: Clock = Clock.System): Flow<LocalDate> = flow {
        while (true) {
            emit(parfaitToday(clock))
            delay(clock.durationUntilNextBoundary())
        }
    }.distinctUntilChanged()
}

/** 반올림으로 경계 직전에 깨어나면 같은 날짜를 다시 낼 수 있어 위에서 걸러 준다 */
private fun Clock.durationUntilNextBoundary(): Duration {
    val now = now()
    val nowDateTime = now.toLocalDateTime(PARFAIT_TIME_ZONE)
    val boundaryTime = LocalTime(DayWindow.DAY_BOUNDARY_HOUR, 0)

    val nextBoundaryDate = if (nowDateTime.time < boundaryTime) {
        nowDateTime.date
    } else {
        nowDateTime.date.plus(1, DateTimeUnit.DAY)
    }

    return nextBoundaryDate.atTime(boundaryTime).toInstant(PARFAIT_TIME_ZONE) - now
}
