package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

class ClearToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    suspend operator fun invoke() = toppingDraftRepository.clear()
}
