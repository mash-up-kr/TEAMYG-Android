package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘 캔버스가 이미 있으면 상세까지 채워 준다. 없으면 `null` 이다.
 *
 * 서버에 `/parfaits/today` 가 있는데도 목록으로 먼저 확인하는 이유는 그 엔드포인트가 조회인데
 * 캔버스를 만들기 때문이다 — 화면을 열기만 해도 빈 캔버스가 저장되고 달력·연도 목록에 그날이
 * 나타난다. 목록과 상세는 둘 다 부작용이 없다.
 *
 * 목록을 오늘 하루로 좁혀 부르므로 30일치를 받아 거르지 않는다. 서버가 상태로 거르지 않아
 * 오늘의 ACTIVE 캔버스도 이 목록에 실려 온다.
 */
class GetTodayParfaitUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(groupId: GroupId): Result<CanvasVO?> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val parfaitId = parfaitRepository
            .getPastCanvases(groupId = groupId, from = today, to = today)
            // 범위를 오늘로 좁혔어도 날짜를 다시 본다 — 경계 처리는 서버 몫이라 하루가 더 딸려
            // 오면 어제 캔버스를 오늘로 착각하게 된다
            .map { canvases -> canvases.firstOrNull { it.date == today }?.parfaitId }
            .getOrElse { throwable -> return Result.failure(throwable) }
            ?: return Result.success(null)

        return parfaitRepository.getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
    }
}
