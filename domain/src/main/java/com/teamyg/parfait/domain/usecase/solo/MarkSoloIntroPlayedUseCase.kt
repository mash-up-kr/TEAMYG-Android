package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.repository.solo.SoloIntroRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class MarkSoloIntroPlayedUseCase @Inject constructor(
    private val soloIntroRepository: SoloIntroRepository,
) {
    suspend operator fun invoke(date: LocalDate) = soloIntroRepository.markPlayed(date)
}
