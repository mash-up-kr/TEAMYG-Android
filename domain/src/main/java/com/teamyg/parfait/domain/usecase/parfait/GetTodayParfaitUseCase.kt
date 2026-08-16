package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

/**
 * 오늘의 캔버스를 받아 온다. 자정 경계를 지나며 요청이 나가 어제 캔버스를 받으면 한 번만
 * 다시 부른다.
 */
class GetTodayParfaitUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(groupId: GroupId): Result<CanvasVO> {
        val canvas = parfaitRepository
            .getTodayCanvas(groupId)
            .getOrElse { throwable -> return Result.failure(throwable) }

        if (canvas.date == today()) return Result.success(canvas)

        // 두 번째도 어긋나면 기기와 서버의 날짜 기준이 다른 것이라 더 불러도 같은 답이 온다
        return parfaitRepository.getTodayCanvas(groupId)
    }

    /** 응답을 받은 뒤에 읽는다 — 요청이 도는 사이 자정을 넘겼다면 그 사이 날짜가 바뀌어 있다 */
    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
