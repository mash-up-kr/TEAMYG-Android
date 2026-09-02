package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoAuth
import com.teamyg.parfait.data.service.model.request.auth.KakaoLoginRequest
import com.teamyg.parfait.data.service.model.request.auth.LogoutRequest
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.service.model.request.auth.SignupRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.auth.KakaoLoginResponse
import com.teamyg.parfait.data.service.model.response.auth.ReissueResponse
import com.teamyg.parfait.data.service.model.response.auth.SignupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @NoAuth
    @POST("api/v1/auth/kakao")
    suspend fun postAuthKakao(@Body request: KakaoLoginRequest): ApiResponse<KakaoLoginResponse>

    @NoAuth
    @POST("api/v1/auth/signup")
    suspend fun postAuthSignup(@Body request: SignupRequest): ApiResponse<SignupResponse>

    @NoAuth
    @POST("api/v1/auth/reissue")
    suspend fun postAuthReissue(@Body request: ReissueRequest): ApiResponse<ReissueResponse>

    @POST("api/v1/auth/logout")
    suspend fun postAuthLogout(@Body request: LogoutRequest)
}
