package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeReadLocalDataSource
import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 목록은 [NoticeLocalDataSource] 인메모리 캐시가 SSoT 다(그룹 목록과 같은 형태).
 * 읽음 기록은 [UserInfoLocalDataSource.myAccount] 의 계정에 묶는다.
 */
class NoticeRepositoryImpl @Inject constructor(
    private val noticeRemoteDataSource: NoticeRemoteDataSource,
    private val noticeLocalDataSource: NoticeLocalDataSource,
    private val noticeReadLocalDataSource: NoticeReadLocalDataSource,
    private val userInfoLocalDataSource: UserInfoLocalDataSource,
) : NoticeRepository {
    override val notices: Flow<List<NoticeVO>?> = noticeLocalDataSource.notices

    // 계정이 바뀌면 이전 계정의 구독을 버리고 새 계정의 키를 읽는다
    @OptIn(ExperimentalCoroutinesApi::class)
    override val readNoticeIds: Flow<Set<Long>> = userInfoLocalDataSource.myAccount
        .map { it?.memberId }
        .distinctUntilChanged()
        .flatMapLatest { memberId ->
            if (memberId == null) flowOf(emptySet()) else noticeReadLocalDataSource.readNoticeIds(memberId)
        }

    // 실패하면 저장하지 않는다 — 이미 받아 둔 목록을 빈 값으로 덮지 않는다
    override suspend fun refreshNotices(): Result<Unit> = noticeRemoteDataSource
        .getNotices()
        .onSuccess(noticeLocalDataSource::saveNotices)
        .map { Unit }
        .mapErrorToAppError()

    override suspend fun markRead(noticeId: Long) {
        currentMemberId()?.let { memberId -> noticeReadLocalDataSource.markRead(memberId, noticeId) }
    }

    override fun clearNotices() = noticeLocalDataSource.clear()

    override suspend fun clearReadNotices() {
        currentMemberId()?.let { memberId -> noticeReadLocalDataSource.clear(memberId) }
    }

    private suspend fun currentMemberId(): MemberId? = userInfoLocalDataSource.myAccount.first()?.memberId
}
