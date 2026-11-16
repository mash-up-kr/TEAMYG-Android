package com.teamyg.parfait.data.source.notice.local

import kotlinx.coroutines.flow.Flow

/**
 * 이 기기에서 상세를 연 공지의 id. 공지는 계정과 무관한 전체 공지라 **기기 단위**로 두고,
 * 로그아웃·세션 만료에도 지우지 않는다.
 */
interface NoticeReadLocalDataSource {
    val readNoticeIds: Flow<Set<Long>>

    suspend fun markRead(id: Long)
}
