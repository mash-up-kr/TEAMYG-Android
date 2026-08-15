package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.ParfaitYearsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ParfaitService {
    @GET("api/v1/groups/{groupId}/parfaits/year")
    suspend fun getGroupsByGroupIdParfaitsYear(@Path("groupId") groupId: Long): ApiResponse<ParfaitYearsResponse>

    /**
     * ⚠️ 조회인데 서버가 캔버스 행을 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다.
     */
    @GET("api/v1/groups/{groupId}/parfaits/today")
    suspend fun getGroupsByGroupIdParfaitsToday(@Path("groupId") groupId: Long): ApiResponse<GetTodayParfaitResponse>

    /**
     * from·to 가 null 이면 Retrofit 이 쿼리 파라미터를 URL 에서 빼므로 서버 기본값
     * (to = 오늘, from = to - 30일)이 그대로 산다.
     */
    @GET("api/v1/groups/{groupId}/parfaits")
    suspend fun getGroupsByGroupIdParfaits(
        @Path("groupId") groupId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): ApiResponse<PastParfaitsResponse>
}
