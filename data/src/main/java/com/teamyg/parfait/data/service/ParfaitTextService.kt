package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.parfaittext.PlaceParfaitTextRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.UpdateParfaitTextRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitTextResponse
import com.teamyg.parfait.data.service.model.response.parfaittext.UpdateParfaitTextResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ParfaitTextService {
    @POST("api/v1/groups/{groupId}/parfaits/{parfaitId}/texts")
    suspend fun postGroupsByGroupIdParfaitsByParfaitIdTexts(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: PlaceParfaitTextRequest,
    ): ApiResponse<TodayParfaitTextResponse>

    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitTextId") parfaitTextId: Long,
        @Body request: UpdateParfaitTextRequest,
    ): ApiResponse<UpdateParfaitTextResponse>

    /** 이미지 토핑 삭제와 같이 204 가 아니라 200 + data: null 이다. */
    @DELETE("api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}")
    suspend fun deleteGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitTextId") parfaitTextId: Long,
    ): ApiResponse<Unit>
}
