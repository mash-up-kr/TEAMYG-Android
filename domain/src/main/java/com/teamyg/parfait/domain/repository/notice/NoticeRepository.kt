package com.teamyg.parfait.domain.repository.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    /** `null` 은 아직 서버에서 한 번도 받지 못했다는 뜻이다. 빈 목록(공지 0건)과 구분한다 */
    val notices: Flow<List<NoticeVO>?>

    /**
     * 현재 계정이 상세를 연 공지의 id. 계정이 없으면 빈 집합이다. 로그아웃해도 지우지 않아
     * 같은 계정으로 다시 들어오면 이어지고, 다른 계정은 0부터 시작한다
     */
    val readNoticeIds: Flow<Set<Long>>

    suspend fun refreshNotices(): Result<Unit>

    /** 현재 계정에 기록한다. 계정이 없으면 귀속할 곳이 없어 기록하지 않는다 */
    suspend fun markRead(id: Long)

    /** 세션이 끝났을 때. 읽음 기록은 건드리지 않는다 */
    fun clearNotices()

    /** 현재 계정의 읽음 기록을 지운다. 로그아웃이 아니라 탈퇴 때만 부른다 */
    suspend fun clearReadNotices()
}
