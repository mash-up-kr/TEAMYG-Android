package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoBodyLog
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 두 엔드포인트 모두 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 *
 * 발급은 리소스를 만드는 POST 인데 201 이 아니라 200 이다(서버가 ApiResponse.ok 를 쓴다).
 * 성공 판정이 success 필드 기반이라 앱에 추가 작업은 없다.
 *
 * 발급 응답 본문에는 presigned uploadUrl 이 실려 온다 — URL 자체가 자격증명이라 본문 로깅을 막는다.
 */
interface ImageService {
    @NoBodyLog
    @POST("api/v1/images")
    suspend fun postImages(@Body request: IssueImageUploadUrlRequest): ApiResponse<IssueImageUploadUrlResponse>

    @POST("api/v1/images/{imageId}/confirm")
    suspend fun postImagesByImageIdConfirm(@Path("imageId") imageId: Long): ApiResponse<ConfirmImageUploadResponse>
}
