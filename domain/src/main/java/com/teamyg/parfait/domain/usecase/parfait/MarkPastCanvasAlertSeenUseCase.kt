package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 계정을 모르면 귀속할 곳이 없어 기록하지 않는다 — 알럿이 한 번 덜 뜨는 쪽이 오귀속보다 낫다 */
class MarkPastCanvasAlertSeenUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val pastCanvasAlertRepository: PastCanvasAlertRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        lastClosedDate: LocalDate,
    ) {
        memberRepository.myAccount.first()?.memberId?.let { memberId ->
            pastCanvasAlertRepository.markSeen(memberId, groupId, lastClosedDate)
        }
    }
}
