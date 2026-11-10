package com.teamyg.parfait.domain.repository.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    /** `null` 은 아직 서버에서 한 번도 받지 못했다는 뜻이다. 빈 목록(공지 0건)과 구분한다 */
    val notices: Flow<List<NoticeVO>?>

    /** 이 기기에서 상세를 연 공지의 id. 로그아웃해도 유지한다 */
    val readNoticeIds: Flow<Set<Long>>

    suspend fun refreshNotices(): Result<Unit>

    suspend fun markRead(id: Long)

    /** 세션이 끝났을 때. 읽음 기록은 건드리지 않는다 */
    fun clearNotices()
}
