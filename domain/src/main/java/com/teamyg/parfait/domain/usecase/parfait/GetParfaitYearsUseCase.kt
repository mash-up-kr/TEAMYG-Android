package com.teamyg.parfait.domain.usecase.parfait

import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

private const val MOCK_YEAR_COUNT = 3

/**
 * 파르페가 하나라도 있는 연도를 최신순으로 준다.
 * 서버의 `GET /api/v1/groups/{groupId}/parfaits/year` 에 대응한다.
 *
 * [GetParfaitHistoriesUseCase] 결과에서 뽑지 않는 이유는 그쪽이 한 해치만 들고 있어서다.
 */
class GetParfaitYearsUseCase
@Inject
constructor() {
    suspend operator fun invoke(): Result<List<Int>> {
        val thisYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year

        // Todo : 서버 연동 시 `groupId` 를 받아 실제 API 를 호출하도록 바꾼다
        val years = (0 until MOCK_YEAR_COUNT).map { thisYear - it }

        return Result.success(years.withYear(thisYear))
    }

    /** 파르페가 없는 해는 서버가 빼고 주지만, 올해는 오늘로 돌아올 수 있어야 해 채워 넣는다 */
    private fun List<Int>.withYear(year: Int): List<Int> = if (contains(year)) {
        this
    } else {
        (this + year).sortedDescending()
    }
}
