package com.teamyg.parfait.core.util.jvm.extension

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * "몇 년 몇 월"을 [LocalDate] 하나로 표현할 때 쓴다 — 연도와 월을 따로 들고 다니면 짝이
 * 어긋나도 타입이 잡아주지 못하고, `LocalDate.monthNumber` 는 이미 deprecated 다.
 */
fun LocalDate.toFirstDayOfMonth(): LocalDate = minus(DatePeriod(days = day - 1))
