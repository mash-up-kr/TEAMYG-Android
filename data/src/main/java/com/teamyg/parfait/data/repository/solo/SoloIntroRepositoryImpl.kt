package com.teamyg.parfait.data.repository.solo

import com.teamyg.parfait.data.source.solo.local.SoloIntroLocalDataSource
import com.teamyg.parfait.domain.repository.solo.SoloIntroRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class SoloIntroRepositoryImpl
@Inject
constructor(
    private val soloIntroLocalDataSource: SoloIntroLocalDataSource,
) : SoloIntroRepository {
    override suspend fun playedDate(): LocalDate? = soloIntroLocalDataSource.playedDate()

    override suspend fun markPlayed(date: LocalDate) = soloIntroLocalDataSource.markPlayed(date)
}
