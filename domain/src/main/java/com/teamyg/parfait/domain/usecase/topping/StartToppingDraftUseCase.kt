package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/** 이전 초안을 남기지 않고 통째로 덮는다(`adr/0026-topping-draft-datastore-ssot.md`) */
class StartToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    suspend operator fun invoke(
        target: ToppingDraftTarget,
        nextPositionZ: Int,
    ) = toppingDraftRepository.start(
        target = target,
        nextPositionZ = nextPositionZ,
    )
}
