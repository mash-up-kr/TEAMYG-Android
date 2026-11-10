package com.teamyg.parfait.data.source.notice.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NoticeService
import com.teamyg.parfait.data.source.notice.mapper.toNoticeVOList
import com.teamyg.parfait.domain.model.notice.NoticeVO
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class NoticeRemoteDataSourceImpl @Inject constructor(
    private val noticeService: NoticeService,
    private val apiCaller: ApiCaller,
) : NoticeRemoteDataSource {
    override suspend fun getNotices(): Result<List<NoticeVO>> = apiCaller
        .safeApiCall(
            block = { noticeService.getNotices(platform = PLATFORM) },
            // 서버 게시일이 KST 기준이라 NEW 판정도 KST 날짜로 한다
            transform = { it.toNoticeVOList(today = LocalDate.now(KST)) },
        )

    private companion object {
        const val PLATFORM = "ANDROID"
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
