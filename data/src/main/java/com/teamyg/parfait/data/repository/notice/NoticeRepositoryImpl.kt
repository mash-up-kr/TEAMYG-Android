package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import javax.inject.Inject

class NoticeRepositoryImpl @Inject constructor(
    private val noticeRemoteDataSource: NoticeRemoteDataSource,
) : NoticeRepository {
    override suspend fun getNotices(): Result<List<NoticeVO>> = noticeRemoteDataSource
        .getNotices()
        .mapErrorToAppError()

    // 서버에 상세 엔드포인트가 없고 목록이 본문까지 담아서 목록에서 찾는다
    override suspend fun getNotice(id: Long): Result<NoticeVO> = getNotices().mapCatching { notices ->
        notices.firstOrNull { it.id == id } ?: throw NoSuchElementException("공지사항을 찾을 수 없다: $id")
    }
}
