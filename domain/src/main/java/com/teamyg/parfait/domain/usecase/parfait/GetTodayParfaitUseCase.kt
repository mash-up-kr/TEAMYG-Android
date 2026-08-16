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
 * 오늘의 캔버스를 상세까지 받아 온다.
 *
 * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
 * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
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

        // 자정을 넘기며 어제 캔버스를 받았다 — 딱 한 번 다시 부른다. 두 번째도 어긋나면 기기와
        // 서버의 날짜 기준이 다른 것이라 더 불러도 같은 답이 오고, 그때는 받은 것을 그대로 쓴다.
        return parfaitRepository.getTodayCanvas(groupId)
    }

    /** 응답을 받은 뒤에 읽는다 — 요청이 도는 사이 자정을 넘겼다면 그 사이 날짜가 바뀌어 있다 */
    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
