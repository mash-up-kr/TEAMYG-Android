package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class UpdateSoloToppingBorderUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(id: ParfaitImageId, border: ToppingBorder): Result<Unit> =
        soloSpaceRepository.updateBorder(id, border)
}
