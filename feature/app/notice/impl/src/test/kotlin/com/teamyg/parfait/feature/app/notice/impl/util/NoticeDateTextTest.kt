package com.teamyg.parfait.feature.app.notice.impl.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NoticeDateTextTest {
    @Test
    fun toNoticeDateText_padsMonthAndDay() {
        assertEquals("2026.01.05", LocalDate(2026, 1, 5).toNoticeDateText())
    }

    @Test
    fun toNoticeDateText_twoDigitMonthAndDay() {
        assertEquals("2026.11.17", LocalDate(2026, 11, 17).toNoticeDateText())
    }
}
