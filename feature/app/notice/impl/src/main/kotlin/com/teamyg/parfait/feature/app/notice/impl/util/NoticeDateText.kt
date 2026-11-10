package com.teamyg.parfait.feature.app.notice.impl.util

import kotlinx.datetime.LocalDate

internal fun LocalDate.toNoticeDateText(): String {
    val monthText = monthNumber.toString().padStart(2, '0')
    val dayText = dayOfMonth.toString().padStart(2, '0')

    return "$year.$monthText.$dayText"
}
