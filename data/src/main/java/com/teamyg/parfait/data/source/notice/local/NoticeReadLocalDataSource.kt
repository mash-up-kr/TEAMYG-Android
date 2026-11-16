package com.teamyg.parfait.data.source.notice.local

import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.coroutines.flow.Flow

/**
 * 계정이 상세를 연 공지의 id. 키는 계정 id 가 아니라 별칭으로 나뉘어 저장되고, 로그아웃·세션
 * 만료에도 지우지 않는다 — 같은 계정으로 다시 들어오면 이어 쓰고 다른 계정은 0부터 시작한다.
 */
interface NoticeReadLocalDataSource {
    fun readNoticeIds(memberId: MemberId): Flow<Set<Long>>

    suspend fun markRead(
        memberId: MemberId,
        id: Long,
    )

    suspend fun clear(memberId: MemberId)
}
