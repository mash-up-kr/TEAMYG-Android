package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.repository.member.UserConfigRepository
import javax.inject.Inject

/** 캔버스 튜토리얼을 끝까지 본 것으로 남긴다. 다음 진입부터 [GetCanvasTutorialVisibleFlowUseCase] 가 `false` 를 낸다 */
class CompleteCanvasTutorialUseCase @Inject constructor(
    private val userConfigRepository: UserConfigRepository,
) {
    suspend operator fun invoke() = userConfigRepository.updateIsShowCanvasTutorial(false)
}
