package com.teamyg.parfait.core.util.jvm.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {
    private val date = LocalDate(2026, 8, 6)

    @Test
    fun fullMonthWithDay_augustSixth_returnsAugust06() {
        // Given 2026-08-06

        // When 전체 월 이름 + 일 포맷 적용
        val formatted = DateFormat.FullMonthWithDay.format(date)

        // Then "August 06" — day()는 기본값이 Padding.ZERO라 0이 붙는다
        assertEquals("August 06", formatted)
    }

    @Test
    fun abbreviatedDayOfWeek_thursday_returnsThu() {
        // Given 2026-08-06 은 목요일

        // When 요일 약어 포맷 적용
        val formatted = DateFormat.AbbreviatedDayOfWeek.format(date)

        // Then "Thu"
        assertEquals("Thu", formatted)
    }

    @Test
    fun monthDayFormat_augustSixth_hasNoDayPadding() {
        // Given 2026-08-06

        // When 축약 월 + 패딩 없는 일 포맷 적용
        val formatted = DateTextFormat.monthDayFormat.format(date)

        // Then "Aug 6" — 앞에 0이 붙지 않는다
        assertEquals("Aug 6", formatted)
    }
}
