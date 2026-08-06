package com.teamyg.parfait.data.source.auth.remote

import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement

interface AuthRemoteDataSource {
    /**
     * @param idToken 카카오 SDK 가 발급한 ID 토큰
     * @param nonce 앱이 생성해 카카오 SDK 요청과 **같은 값**을 보내야 한다
     */
    suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO>

    suspend fun signup(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO>

    suspend fun reissue(refreshToken: RefreshToken): Result<AuthSessionVO>

    suspend fun logout(refreshToken: RefreshToken): Result<Unit>
}
