package com.teamyg.parfait.domain.usecase.notice

import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 현재 계정이 읽은 공지 id. 계정이 없으면 빈 집합이다.
 *
 * 계정이 바뀌면 이전 계정의 구독을 버리고 새 계정의 기록을 읽는다. 계정 판단은 여기서 하고
 * [NoticeRepository] 는 받은 `memberId` 로 저장·조회만 한다.
 */
class GetReadNoticeIdsUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val noticeRepository: NoticeRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Set<Long>> = memberRepository.myAccount
        .map { it?.memberId }
        .distinctUntilChanged()
        .flatMapLatest { memberId ->
            if (memberId == null) flowOf(emptySet()) else noticeRepository.readNoticeIds(memberId)
        }
}
