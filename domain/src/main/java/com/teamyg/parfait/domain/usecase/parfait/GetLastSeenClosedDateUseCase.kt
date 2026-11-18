package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 계정을 모르면 `null` — 처음 확인과 같게 다뤄 기준선만 세우고 알리지 않는다 */
class GetLastSeenClosedDateUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val pastCanvasAlertRepository: PastCanvasAlertRepository,
) {
    suspend operator fun invoke(groupId: GroupId): LocalDate? = memberRepository.myAccount
        .first()
        ?.memberId
        ?.let { memberId -> pastCanvasAlertRepository.lastSeenClosedDate(memberId, groupId) }
}
