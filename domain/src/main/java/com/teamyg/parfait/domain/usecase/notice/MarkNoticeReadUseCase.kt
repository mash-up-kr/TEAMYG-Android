package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 계정이 없으면 귀속할 곳이 없어 기록하지 않는다 — NEW 칩이 한 번 더 보이는 쪽이 낫다 */
class MarkNoticeReadUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val noticeRepository: NoticeRepository,
) {
    suspend operator fun invoke(noticeId: Long) {
        memberRepository.myAccount.first()?.memberId?.let { memberId ->
            noticeRepository.markRead(memberId, noticeId)
        }
    }
}
