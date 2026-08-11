package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageResponse
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 *
 * 경로의 images 세그먼트는 최상위 /api/v1/images(업로드)와 다른 도메인이다 —
 * 이쪽은 캔버스 배치다.
 */
interface ParfaitImageService {
    @POST("api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
    suspend fun postGroupsByGroupIdParfaitsByParfaitIdImages(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: PlaceParfaitImageRequest,
    ): ApiResponse<PlaceParfaitImageResponse>

    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
        @Body request: UpdateParfaitImageRequest,
    ): ApiResponse<UpdateParfaitImageResponse>
}
