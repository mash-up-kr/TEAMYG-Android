package com.teamyg.parfait.core.util.jvm.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTextFormatTest {
    private val date = LocalDate(2026, 8, 6)

    @Test
    fun monthDayFormat_augustSixth_hasNoDayPadding() {
        // When 축약 월 + 패딩 없는 일 포맷 적용
        val formatted = DateTextFormat.monthDayFormat.format(date)

        // Then "Aug 6" — 앞에 0이 붙지 않는다
        assertEquals("Aug 6", formatted)
    }

    @Test
    fun dottedDateFormat_singleDigitMonthAndDay_arePadded() {
        // When 점 구분 포맷 적용
        val formatted = DateTextFormat.dottedDateFormat.format(LocalDate(2026, 1, 5))

        // Then 월·일이 두 자리로 채워진다
        assertEquals("2026.01.05", formatted)
    }

    @Test
    fun dottedDateFormat_twoDigitMonthAndDay_keepsDigits() {
        assertEquals("2026.11.17", DateTextFormat.dottedDateFormat.format(LocalDate(2026, 11, 17)))
    }
}
