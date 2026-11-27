package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoAuth
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.appversion.AppVersionCheckResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AppVersionService {
    @NoAuth
    @GET("api/v1/app-versions/check")
    suspend fun checkVersion(
        @Query("platform") platform: String,
        @Query("version") version: String,
    ): ApiResponse<AppVersionCheckResponse>
}
