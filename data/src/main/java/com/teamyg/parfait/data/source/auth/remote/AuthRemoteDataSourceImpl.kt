package com.teamyg.parfait.data.source.auth.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.KakaoLoginRequest
import com.teamyg.parfait.data.service.model.request.auth.LogoutRequest
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.service.model.request.auth.SignupRequest
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.auth.mapper.toKakaoLoginVO
import com.teamyg.parfait.data.source.auth.mapper.toRequest
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authService: AuthService,
    private val apiCaller: ApiCaller,
) : AuthRemoteDataSource {
    override suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO> = apiCaller
        .safeApiCall(
            block = {
                authService.postAuthKakao(
                    request = KakaoLoginRequest(
                        idToken = idToken,
                        nonce = nonce,
                    ),
                )
            },
            transform = { it.toKakaoLoginVO() },
        )

    override suspend fun signup(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO> = apiCaller
        .safeApiCall(
            block = {
                authService.postAuthSignup(
                    SignupRequest(
                        registrationToken = registrationToken.value,
                        agreements = agreements.map { it.toRequest() },
                    ),
                )
            },
            transform = { it.toAuthSessionVO() },
        )

    override suspend fun reissue(refreshToken: RefreshToken): Result<AuthSessionVO> = apiCaller
        .safeApiCall(
            block = {
                authService.postAuthReissue(
                    request = ReissueRequest(
                        refreshToken = refreshToken.value,
                    ),
                )
            },
            transform = { it.toAuthSessionVO() },
        )

    override suspend fun logout(refreshToken: RefreshToken): Result<Unit> = apiCaller
        .safeApiCallNoContent {
            authService.postAuthLogout(
                request = LogoutRequest(
                    refreshToken = refreshToken.value,
                ),
            )
        }
}
