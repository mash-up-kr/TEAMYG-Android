package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 오늘 캔버스 갱신의 결론이 났는지 구독한다 — 성공이든 실패든 한 번 끝나면 `true` 다.
 *
 * [GetTodayParfaitFlowUseCase] 는 캔버스가 실릴 때만 값을 내므로, 그것만 보면 조회가 실패한
 * 화면은 초기 로딩에 갇힌다. 덮개를 걷을 근거는 캔버스의 도착이 아니라 이 신호다.
 */
class ObserveTodayParfaitSettledUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    operator fun invoke(groupId: GroupId): Flow<Boolean> = parfaitRepository.isTodayCanvasSettled(groupId)
}
