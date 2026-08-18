package com.teamyg.parfait.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 파르페의 하루가 놓이는 시간대.
 *
 * 하루를 가르는 것은 기기가 아니라 서버다 — 캔버스 행이 KST 날짜를 키로 저장되고
 * (`TZ=Asia/Seoul`), 오늘 조회도 서버가 그 날짜로 캔버스를 찾는다. 기기 시간대로 오늘을 세면
 * 해외에 있는 기기에서 서버와 하루가 어긋나, 서버가 준 캔버스를 어제 것으로 오해하거나
 * 달력이 오늘을 미래로 보고 잠근다.
 */
val PARFAIT_TIME_ZONE: TimeZone = TimeZone.of("Asia/Seoul")

/**
 * 파르페 기준의 오늘. 기기 시간대를 따르지 않는 이유는 [PARFAIT_TIME_ZONE] 에 있다.
 *
 * 하루는 자정이 아니라 **새벽 3시**에 넘어간다 — 캔버스 마감 배치가 도는 시각이라
 * 자정~03시 사이에는 아직 전날 캔버스가 진행 중이다. 서버 `ParfaitDay.current()` 의 거울이고,
 * 계약이 그 값을 내려주지 않아 앱이 복제하고 있다. **서버가 배치 시각을 바꾸면 여기도 바꾼다.**
 * 경계 값은 [DayWindow.DAY_BOUNDARY_HOUR] 하나만 쓴다 — 두 곳에 적으면 한쪽만 고쳐진다.
 */
fun parfaitToday(clock: Clock = Clock.System): LocalDate {
    val now = clock.now().toLocalDateTime(PARFAIT_TIME_ZONE)

    return if (now.time < LocalTime(DayWindow.DAY_BOUNDARY_HOUR, 0)) {
        now.date.minus(1, DateTimeUnit.DAY)
    } else {
        now.date
    }
}
