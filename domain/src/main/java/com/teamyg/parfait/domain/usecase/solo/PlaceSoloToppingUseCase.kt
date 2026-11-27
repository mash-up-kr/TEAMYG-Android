package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class PlaceSoloToppingUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(
        filePath: String,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<Unit> = soloSpaceRepository.placeTopping(filePath, transform, border)
}
