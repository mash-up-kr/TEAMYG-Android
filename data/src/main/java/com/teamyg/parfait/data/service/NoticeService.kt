package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 상세 엔드포인트는 없다 — 목록 응답이 본문까지 담는다(`docs/api/notice.md`).
 */
interface NoticeService {
    @GET("api/v1/notices")
    suspend fun getNotices(@Query("platform") platform: String): ApiResponse<List<NoticeResponse>>
}
