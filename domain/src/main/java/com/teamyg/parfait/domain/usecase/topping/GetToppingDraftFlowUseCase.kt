package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetToppingDraftFlowUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    operator fun invoke(): Flow<ToppingDraft?> = toppingDraftRepository.draft
}
