package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.CanvasIntroRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 계정을 모르면 귀속할 곳이 없어 기록하지 않는다 — 연출이 한 번 더 뜨는 쪽이 오귀속보다 낫다 */
class MarkCanvasIntroPlayedUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val canvasIntroRepository: CanvasIntroRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        date: LocalDate,
    ) {
        memberRepository.myAccount.first()?.memberId?.let { memberId ->
            canvasIntroRepository.markPlayed(memberId, groupId, date)
        }
    }
}
