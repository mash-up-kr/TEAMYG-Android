package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 고른 날의 캔버스를 상세까지 받아 온다. 그날 캔버스가 아예 없으면 `null` 이다.
 *
 * 오늘을 골라도 `/parfaits/today` 를 타지 않는다 — 그쪽은 없으면 캔버스를 만들어 저장하므로,
 * 달력을 훑는 것만으로 빈 캔버스가 생기면 안 된다. 목록·상세는 둘 다 부작용이 없다.
 */
class GetCanvasByDateUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        date: LocalDate,
    ): Result<CanvasVO?> {
        val parfaitId = parfaitRepository
            .getPastCanvases(groupId = groupId, from = date, to = date)
            // 범위를 하루로 좁혔어도 날짜를 다시 본다 — 경계 처리는 서버 몫이라 하루가 더 딸려
            // 오면 옆날 캔버스를 고른 날로 착각하게 된다
            .map { canvases -> canvases.firstOrNull { it.date == date }?.parfaitId }
            .getOrElse { throwable -> return Result.failure(throwable) }
            ?: return Result.success(null)

        return parfaitRepository.getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
    }
}
