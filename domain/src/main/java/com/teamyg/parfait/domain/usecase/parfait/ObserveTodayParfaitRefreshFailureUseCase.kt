package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 오늘 캔버스 갱신 실패 구독. 첫 조회를 기다리는 화면이 로딩을 푸는 계기로 쓴다.
 *
 * 갱신이 실패하면 캐시가 비어 있는 채라 [GetTodayParfaitFlowUseCase] 는 아무것도 내지 않는다 —
 * 그 화면은 이 신호가 없으면 로딩에 갇힌다.
 */
class ObserveTodayParfaitRefreshFailureUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    operator fun invoke(groupId: GroupId): Flow<Unit> = parfaitRepository.todayCanvasRefreshFailures(groupId)
}
