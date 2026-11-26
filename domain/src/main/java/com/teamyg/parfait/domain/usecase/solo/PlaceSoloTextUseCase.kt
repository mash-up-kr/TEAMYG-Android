package com.teamyg.parfait.domain.usecase.solo

import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import javax.inject.Inject

class PlaceSoloTextUseCase @Inject constructor(
    private val soloSpaceRepository: SoloSpaceRepository,
) {
    suspend operator fun invoke(text: String, transform: ToppingTransform, style: TextToppingStyle): Result<Unit> =
        soloSpaceRepository.placeText(text, transform, style)
}
