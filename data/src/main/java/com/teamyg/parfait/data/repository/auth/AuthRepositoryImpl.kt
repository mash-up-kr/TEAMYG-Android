package com.teamyg.parfait.data.repository.auth

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.auth.RefreshToken
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.auth.TermsAgreement
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 원격 인증 호출과 토큰 저장을 묶는다.
 *
 * 실패 원인을 여기서 [com.teamyg.parfait.domain.model.error.AppError] 로 바꾼다 —
 * 이 경계가 있어야 feature 모듈이 `:data` 의 `ApiException` 을 보지 않는다.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val tokenStore: TokenStore,
) : AuthRepository {
    override suspend fun loginWithKakao(
        idToken: String,
        nonce: String,
    ): Result<KakaoLoginVO> = authRemoteDataSource
        .loginWithKakao(idToken = idToken, nonce = nonce)
        .mapErrorToAppError()

    override suspend fun signUp(
        registrationToken: RegistrationToken,
        agreements: List<TermsAgreement>,
    ): Result<AuthSessionVO> = authRemoteDataSource
        .signup(
            registrationToken = registrationToken,
            agreements = agreements,
        ).mapErrorToAppError()

    override suspend fun saveSession(session: AuthSessionVO) {
        tokenStore.save(
            accessToken = session.accessToken.value,
            refreshToken = session.refreshToken.value,
        )
    }

    override suspend fun logout(): Result<Unit> {
        val refreshToken = tokenStore.getRefreshToken()

        if (refreshToken != null) {
            authRemoteDataSource
                .logout(RefreshToken(refreshToken))
                .onFailure { throwable -> repositoryLogger.e(throwable) { "서버 로그아웃 실패 — 로컬은 정리한다" } }
        }

        tokenStore.clear()
        return Result.success(Unit)
    }
}
