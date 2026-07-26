package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.TempResponse
import retrofit2.http.GET
import retrofit2.http.Path

// TODO 실제 API 확정 시 삭제 (구조 예시용)
interface TempService {
    @GET("temp/{id}")
    suspend fun getTemp(@Path("id") id: String): ApiResponse<TempResponse>
}
