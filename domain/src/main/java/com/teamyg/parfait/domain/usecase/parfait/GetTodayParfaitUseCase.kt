package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘의 캔버스를 받아 온다. 파르페 하루 경계(새벽 3시)를 지나며 요청이 나가 어제 캔버스를
 * 받으면 한 번만 다시 부른다.
 */
class GetTodayParfaitUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    /**
     * @param clock 파르페 하루 경계 판정에 쓰는 시계. 테스트에서 주입해 경계 앞뒤 시각을
     * 고정할 수 있고, 운영 호출부는 기본값인 시스템 시계를 그대로 쓴다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        clock: Clock = Clock.System,
    ): Result<CanvasVO> {
        val canvas = parfaitRepository
            .getTodayCanvas(groupId)
            .getOrElse { throwable -> return Result.failure(throwable) }

        // 오늘을 응답 뒤에 읽는다 — 요청이 도는 사이 파르페 하루 경계를 넘겼다면 그 사이 날짜가 바뀌어 있다
        if (canvas.date == parfaitToday(clock)) return Result.success(canvas)

        // 두 번째도 어긋나면 기기와 서버의 시계가 어긋난 것이라 더 불러도 같은 답이 온다
        return parfaitRepository.getTodayCanvas(groupId)
    }
}
