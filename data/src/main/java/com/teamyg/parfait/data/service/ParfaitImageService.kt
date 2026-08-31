package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageBorderRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImagesRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageBorderResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImagesResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    /**
     * POST 와 경로가 같고 메서드만 다르다 — 서버에서도 두 컨트롤러가 이 URL 을 나눠 갖는다.
     */
    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImages(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: UpdateParfaitImagesRequest,
    ): ApiResponse<UpdateParfaitImagesResponse>

    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}/border")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
        @Body request: UpdateParfaitImageBorderRequest,
    ): ApiResponse<UpdateParfaitImageBorderResponse>

    /**
     * 성공이 204 가 아니라 200 + data: null 이다 — 회원 탈퇴(DELETE /users/me)와 달리
     * envelope 가 온다(`api/conventions.md`).
     */
    @DELETE("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}")
    suspend fun deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
    ): ApiResponse<Unit>
}
