package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.notice.local.NoticeLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeReadLocalDataSource
import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 목록은 [NoticeLocalDataSource] 인메모리 캐시가 SSoT 다. 조회는 캐시를 읽는 [Flow] 하나,
 * 서버 재조회는 [refreshNotices] 로 갈라 둔다(그룹 목록과 같은 형태).
 */
class NoticeRepositoryImpl @Inject constructor(
    private val noticeRemoteDataSource: NoticeRemoteDataSource,
    private val noticeLocalDataSource: NoticeLocalDataSource,
    private val noticeReadLocalDataSource: NoticeReadLocalDataSource,
) : NoticeRepository {
    override val notices: Flow<List<NoticeVO>?> = noticeLocalDataSource.notices

    override val readNoticeIds: Flow<Set<Long>> = noticeReadLocalDataSource.readNoticeIds

    // 실패하면 저장하지 않는다 — 이미 받아 둔 목록을 빈 값으로 덮지 않는다
    override suspend fun refreshNotices(): Result<Unit> = noticeRemoteDataSource
        .getNotices()
        .onSuccess(noticeLocalDataSource::saveNotices)
        .map { }
        .mapErrorToAppError()

    override suspend fun markRead(id: Long) = noticeReadLocalDataSource.markRead(id)

    override fun clearNotices() = noticeLocalDataSource.clear()
}
