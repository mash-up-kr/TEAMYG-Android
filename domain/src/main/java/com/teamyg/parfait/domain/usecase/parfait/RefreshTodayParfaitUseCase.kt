package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘의 캔버스를 받아 캐시에 싣는다. 파르페 하루 경계(새벽 3시)를 지나며 요청이 나가
 * 어제 캔버스를 받으면 한 번만 다시 부른다.
 *
 * 값은 돌려주지 않는다 — 읽는 길은 [GetTodayParfaitFlowUseCase] 하나다.
 */
class RefreshTodayParfaitUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    /**
     * @param clock 파르페 하루 경계 판정에 쓰는 시계. 테스트에서 경계 앞뒤 시각을 고정한다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        clock: Clock = Clock.System,
    ): Result<Unit> {
        val first = parfaitRepository.refreshTodayCanvas(groupId)
        if (first.isFailure) return first

        // 오늘을 응답 뒤에 읽는다 — 요청이 도는 사이 하루 경계를 넘겼다면 어제 것이 실려 있다.
        // 구독이 아니라 peek 을 쓰는 이유는 cachedTodayCanvasDate KDoc 에 있다
        if (parfaitRepository.cachedTodayCanvasDate(groupId) == parfaitToday(clock)) return first

        // 두 번째도 어긋나면 기기와 서버의 시계가 어긋난 것이라 더 불러도 같은 답이 온다
        return parfaitRepository.refreshTodayCanvas(groupId)
    }
}
