package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Task 4 에서 서버·캐시 구현으로 교체한다. 그 전까지 :data 가 컴파일되도록 둔 자리표시다.
class NoticeRepositoryImpl @Inject constructor() : NoticeRepository {
    override val notices: Flow<List<NoticeVO>?> get() = TODO()
    override val readNoticeIds: Flow<Set<Long>> get() = TODO()

    override suspend fun refreshNotices(): Result<Unit> = TODO()

    override suspend fun markRead(id: Long) = TODO()

    override fun clearNotices() = TODO()
}
