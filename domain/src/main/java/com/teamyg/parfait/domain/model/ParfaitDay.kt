package com.teamyg.parfait.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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

/** 파르페 기준의 오늘. 기기 시간대를 따르지 않는 이유는 [PARFAIT_TIME_ZONE] 에 있다 */
fun parfaitToday(clock: Clock = Clock.System): LocalDate = clock.todayIn(PARFAIT_TIME_ZONE)
