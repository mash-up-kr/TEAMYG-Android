package com.teamyg.parfait.data.source.notice.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NoticeService
import com.teamyg.parfait.data.source.notice.mapper.toNoticeVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import javax.inject.Inject

class NoticeRemoteDataSourceImpl @Inject constructor(
    private val noticeService: NoticeService,
    private val apiCaller: ApiCaller,
) : NoticeRemoteDataSource {
    override suspend fun getNotices(): Result<List<NoticeVO>> = apiCaller
        .safeApiCall(
            block = { noticeService.getNotices(platform = PLATFORM) },
            transform = { notices -> notices.map { it.toNoticeVO() } },
        )

    private companion object {
        const val PLATFORM = "ANDROID"
    }
}
