package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class UpdateSoloTransformsUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(
        updates: List<ToppingTransformUpdate>,
        textUpdates: List<TextToppingTransformUpdate>,
    ): Result<Unit> = soloSpaceRepository.updateTransforms(updates, textUpdates)
}
