package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class DeleteSoloToppingUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(id: ParfaitImageId): Result<Unit> = soloSpaceRepository.deleteTopping(id)
}
