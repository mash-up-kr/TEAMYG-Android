package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/**
 * 파르페가 하나라도 있는 연도를 최신순으로 준다.
 *
 * [GetParfaitHistoriesUseCase] 결과에서 뽑지 않는 이유는 그쪽이 한 해치만 들고 있어서다.
 */
class GetParfaitYearsUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(groupId: GroupId): Result<List<Int>> {
        val thisYear = parfaitToday().year

        return parfaitRepository.getYears(groupId).map { years -> years.withYear(thisYear) }
    }

    /** 파르페가 없는 해는 서버가 빼고 주지만, 올해는 오늘로 돌아올 수 있어야 해 채워 넣는다 */
    private fun List<Int>.withYear(year: Int): List<Int> = if (contains(year)) {
        this
    } else {
        (this + year).sortedDescending()
    }
}
