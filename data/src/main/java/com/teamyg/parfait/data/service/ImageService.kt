package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoBodyLog
import com.teamyg.parfait.data.service.model.request.image.IssueImageUploadUrlRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ImageService {
    @NoBodyLog
    @POST("api/v1/images")
    suspend fun postImages(@Body request: IssueImageUploadUrlRequest): ApiResponse<IssueImageUploadUrlResponse>

    @POST("api/v1/images/{imageId}/confirm")
    suspend fun postImagesByImageIdConfirm(@Path("imageId") imageId: Long): ApiResponse<ConfirmImageUploadResponse>
}
