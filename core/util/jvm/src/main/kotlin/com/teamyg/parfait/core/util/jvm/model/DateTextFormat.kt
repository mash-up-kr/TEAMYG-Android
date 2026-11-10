package com.teamyg.parfait.core.util.jvm.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding

object DateTextFormat {
    val weekdayFormat = LocalDate.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
    }

    val monthDayFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        chars(" ")
        day(padding = Padding.NONE)
    }

    /** `2026.11.17` — 월·일은 두 자리로 채운다 */
    val dottedDateFormat = LocalDate.Format {
        year()
        chars(".")
        monthNumber(padding = Padding.ZERO)
        chars(".")
        day(padding = Padding.ZERO)
    }

    val monthFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
    }

    val fullMonthFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_FULL)
    }
}
