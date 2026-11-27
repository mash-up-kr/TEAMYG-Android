package com.teamyg.parfait.data.source.solo.local

import kotlinx.datetime.LocalDate

interface SoloIntroLocalDataSource {
    suspend fun playedDate(): LocalDate?

    suspend fun markPlayed(date: LocalDate)
}
