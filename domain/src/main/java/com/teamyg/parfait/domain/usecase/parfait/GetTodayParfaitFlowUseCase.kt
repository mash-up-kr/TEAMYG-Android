package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘 캔버스 구독. 캐시에 실린 것이 오늘 날짜가 아니면 `null` 로 낸다.
 *
 * ⚠️ 이 필터는 업스트림이 방출할 때만 평가된다. 캐시에 `distinctUntilChanged` 가 걸려 있어
 * 값이 안 바뀌면 재방출이 없다 — 화면을 열어 둔 채 하루 경계를 넘기는 경우는 이 필터가 아니라
 * 별도 시간 축이 닫는다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
 */
class GetTodayParfaitFlowUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    /**
     * @param clock 하루 경계 판정에 쓰는 시계. 주입하지 않으면 낡음 필터를 테스트로 고정할 수 없다.
     */
    operator fun invoke(
        groupId: GroupId,
        clock: Clock = Clock.System,
    ): Flow<CanvasVO?> = parfaitRepository
        .todayCanvas(groupId)
        .map { canvas -> canvas?.takeIf { it.date == parfaitToday(clock) } }
}
