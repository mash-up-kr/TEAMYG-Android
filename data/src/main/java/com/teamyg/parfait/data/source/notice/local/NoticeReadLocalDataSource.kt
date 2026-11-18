package com.teamyg.parfait.data.source.notice.local

import com.teamyg.parfait.domain.model.id.MemberId
import kotlinx.coroutines.flow.Flow

/** 계정이 상세를 연 공지의 id 를 계정 별칭 키로 저장한다. 로그아웃·세션 만료에도 지우지 않는다. */
interface NoticeReadLocalDataSource {
    fun readNoticeIds(memberId: MemberId): Flow<Set<Long>>

    suspend fun markRead(
        memberId: MemberId,
        noticeId: Long,
    )

    suspend fun clear(memberId: MemberId)
}
