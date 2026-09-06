package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO
import com.teamyg.parfait.domain.repository.member.UserConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 이 튜토리얼을 지금 보여줘야 하는지 구독한다.
 *
 * 설정이 아예 없는 상태(앱 설치 직후)를 "아직 안 봤다"로 읽는 곳이 여기다 — [UserConfigVO] 의
 * 기본값과 같은 판단을 화면마다 다시 쓰지 않게 한 곳에 모아 둔다.
 */
class GetTutorialVisibleFlowUseCase @Inject constructor(
    private val userConfigRepository: UserConfigRepository,
) {
    operator fun invoke(tutorial: TutorialKind): Flow<Boolean> = userConfigRepository.userConfig
        .map { config -> (config ?: UserConfigVO()).isTutorialVisible(tutorial) }
        .distinctUntilChanged()
}
