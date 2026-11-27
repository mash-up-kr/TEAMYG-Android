package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class DeleteSoloSpaceUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(): Result<Unit> = soloSpaceRepository.delete()
}
