package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** 토핑 만들기 흐름의 초안 구독. 호출 자체는 구독하지 않고 `Flow` 만 넘긴다 */
class GetToppingDraftFlowUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    operator fun invoke(): Flow<ToppingDraft?> = toppingDraftRepository.draft
}
