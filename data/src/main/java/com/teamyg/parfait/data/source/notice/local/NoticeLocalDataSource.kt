package com.teamyg.parfait.data.source.notice.local

import com.teamyg.parfait.domain.model.notice.NoticeVO
import kotlinx.coroutines.flow.StateFlow

/**
 * 공지 목록의 인메모리 SSoT. 서버가 정본이라 디스크에 남기지 않고 프로세스와 수명을 같이 한다
 * (`docs/adr/0023-group-in-memory-ssot.md` 와 같은 판단).
 */
interface NoticeLocalDataSource {
    /** `null` 은 아직 한 번도 받지 못했다는 뜻이다. 빈 목록(공지 0건)과 구분한다 */
    val notices: StateFlow<List<NoticeVO>?>

    fun saveNotices(notices: List<NoticeVO>)

    /** 세션이 끝났을 때. 미수신 상태로 되돌린다 */
    fun clear()
}
