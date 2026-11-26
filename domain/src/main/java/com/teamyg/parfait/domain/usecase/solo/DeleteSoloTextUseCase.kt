package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class DeleteSoloTextUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(id: ParfaitTextId): Result<Unit> = soloSpaceRepository.deleteText(id)
}
