package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.parfait.ChangeParfaitBackgroundRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.ChangeParfaitBackgroundResponse
import com.teamyg.parfait.data.service.model.response.parfait.GetTodayParfaitResponse
import com.teamyg.parfait.data.service.model.response.parfait.ParfaitYearsResponse
import com.teamyg.parfait.data.service.model.response.parfait.PastParfaitsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ParfaitService {
    @GET("api/v1/groups/{groupId}/parfaits/year")
    suspend fun getGroupsByGroupIdParfaitsYear(@Path("groupId") groupId: Long): ApiResponse<ParfaitYearsResponse>

    /** 조회인데 오늘 날짜 파르페가 없으면 서버가 만들어 저장한다 */
    @GET("api/v1/groups/{groupId}/parfaits/today")
    suspend fun getGroupsByGroupIdParfaitsToday(@Path("groupId") groupId: Long): ApiResponse<GetTodayParfaitResponse>

    /**
     * from·to 가 null 이면 서버 기본값이 쓰인다 — `to` 는 서버 기준 오늘(03시에 넘어간다), `from` 은
     * 그로부터 30일 전이다. 앱은 항상 범위를 명시해 부른다.
     */
    @GET("api/v1/groups/{groupId}/parfaits")
    suspend fun getGroupsByGroupIdParfaits(
        @Path("groupId") groupId: Long,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): ApiResponse<PastParfaitsResponse>

    /**
     * 특정 캔버스 상세. 응답이 오늘 조회와 같은 타입이다.
     *
     * 오늘 조회와 달리 부작용이 없다. 상태로 거르지 않아 오늘의 ACTIVE 캔버스도 이 경로로 온다
     * (`docs/api/parfait.md`).
     *
     * 파르페가 없거나 다른 그룹 소속이면 404 PARFAIT_NOT_FOUND 다(403 이 아니다).
     */
    @GET("api/v1/groups/{groupId}/parfaits/{parfaitId}")
    suspend fun getGroupsByGroupIdParfaitsByParfaitId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
    ): ApiResponse<GetTodayParfaitResponse>

    /**
     * 마감된 캔버스(CLOSED·EMPTY)면 409 PARFAIT_ALREADY_CLOSED 다.
     *
     * 배경 이미지는 image_meta.reference_count 를 올리지 않는다. 같은 이미지를 토핑으로도 올렸다가
     * 그 토핑을 지우면 S3 객체가 삭제돼 배경이 깨진다(`docs/api/parfait.md`).
     */
    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/background")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdBackground(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: ChangeParfaitBackgroundRequest,
    ): ApiResponse<ChangeParfaitBackgroundResponse>
}
