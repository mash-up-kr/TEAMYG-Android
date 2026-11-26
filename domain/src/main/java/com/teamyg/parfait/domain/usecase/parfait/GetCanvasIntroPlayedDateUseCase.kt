package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.CanvasIntroRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/** 계정을 모르면 `null` — 아직 재생하지 않은 것과 같게 다룬다 */
class GetCanvasIntroPlayedDateUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val canvasIntroRepository: CanvasIntroRepository,
) {
    suspend operator fun invoke(groupId: GroupId): LocalDate? = memberRepository.myAccount
        .first()
        ?.memberId
        ?.let { memberId -> canvasIntroRepository.playedDate(memberId, groupId) }
}
