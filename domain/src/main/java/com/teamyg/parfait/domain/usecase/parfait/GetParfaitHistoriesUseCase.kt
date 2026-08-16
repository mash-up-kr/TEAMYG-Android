package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject

private const val JANUARY = 1
private const val FIRST_DAY_OF_MONTH = 1

/**
 * 한 해 동안 그룹이 남긴 캔버스 목록을 최신순으로 준다.
 *
 * 연 단위로 끊는 이유는 달력이 연·월을 오가며 그려지기 때문이다 — 월이 바뀔 때마다 다시
 * 부르지 않아도 되고, 호출부가 연도별로 캐시해 두기도 쉽다. 목록 API 는 페이지네이션도
 * 범위 상한도 없어 한 해치(최대 366건)가 한 번에 온다.
 */
class GetParfaitHistoriesUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        year: Int,
    ): Result<List<PastCanvasVO>> {
        val firstDay = LocalDate(year, JANUARY, FIRST_DAY_OF_MONTH)
        // 다음 해 1월 1일에서 하루를 빼 12월 31일을 얻는다 — 월별 말일을 따로 다루지 않는다
        val lastDay = firstDay.plus(DatePeriod(years = 1)).minus(DatePeriod(days = 1))

        return parfaitRepository
            .getPastCanvases(groupId = groupId, from = firstDay, to = lastDay)
            // 서버가 순서를 약속하지 않아 여기서 정한다
            .map { canvases -> canvases.sortedByDescending(PastCanvasVO::date) }
    }
}
