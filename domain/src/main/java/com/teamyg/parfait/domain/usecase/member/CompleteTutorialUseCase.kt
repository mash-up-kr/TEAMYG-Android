package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.repository.member.UserConfigRepository
import javax.inject.Inject

/** 이 튜토리얼을 끝까지 본 것으로 남긴다. 다음 진입부터 [GetTutorialVisibleFlowUseCase] 가 `false` 를 낸다 */
class CompleteTutorialUseCase @Inject constructor(
    private val userConfigRepository: UserConfigRepository,
) {
    suspend operator fun invoke(tutorial: TutorialKind) = userConfigRepository.markTutorialSeen(tutorial)
}
