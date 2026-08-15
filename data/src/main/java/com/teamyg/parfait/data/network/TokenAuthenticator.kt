package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 401 을 가로채 access token 을 재발급하고 원요청을 다시 만든다.
 *
 * [authService] 가 `Provider` 인 이유: `Retrofit` 이 `OkHttpClient` 를, `OkHttpClient` 가 이
 * 인증기를 요구해 직접 주입하면 Dagger 순환이다. 지연 주입으로 끊는다.
 *
 * `runBlocking` 을 쓰는 이유: [Authenticator] 계약이 동기다. 저장소 읽기·재발급이 모두
 * `suspend` 라 다른 길이 없다 — `TokenStoreTokenProvider` 도 같은 이유로 같은 방식이다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val authService: Provider<AuthService>,
    private val apiCaller: ApiCaller,
    private val sessionEventBus: SessionEventBus,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? = runBlocking {
        mutex.withLock {
            val refreshToken = tokenStore.getRefreshToken() ?: return@withLock null

            val session = reissue(refreshToken) ?: return@withLock null
            tokenStore.save(
                accessToken = session.accessToken.value,
                refreshToken = session.refreshToken.value,
            )
            response.request.withToken(session.accessToken.value)
        }
    }

    private suspend fun reissue(refreshToken: String): AuthSessionVO? = apiCaller
        .safeApiCall(
            block = { authService.get().postAuthReissue(ReissueRequest(refreshToken = refreshToken)) },
            transform = { it.toAuthSessionVO() },
        ).getOrElse { throwable ->
            if (throwable.isSessionDead()) {
                sourceLogger.e(throwable) { "재발급 거절 — 세션 종료" }
                tokenStore.clear()
                sessionEventBus.postForcedLogout()
            } else {
                // 연결 실패·서버 장애로 2주짜리 refresh token 을 버리지 않는다.
                // 원요청은 401 그대로 화면에 도달하고 화면이 실패를 표시한다.
                sourceLogger.e(throwable) { "재발급 실패 — 세션 유지" }
            }
            null
        }

    /**
     * 서버가 refresh token 자체를 거절했는가.
     *
     * `statusCode` 와 `code` 를 함께 보는 이유: envelope 실패는 `statusCode` 가 비어 올 수
     * 있고(`ApiCaller.runCatchingApi`), HTTP 실패는 code 없이 status 만 온다.
     */
    private fun Throwable.isSessionDead(): Boolean = when (this) {
        is ApiException.Business -> statusCode in SESSION_DEAD_STATUS_CODES || code in SESSION_DEAD_CODES
        is ApiException.Http -> statusCode in SESSION_DEAD_STATUS_CODES
        else -> false
    }

    private fun Request.withToken(accessToken: String): Request = newBuilder()
        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
        .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "

        val SESSION_DEAD_STATUS_CODES = setOf(401, 403)

        val SESSION_DEAD_CODES = setOf(
            ServerErrorCode.Auth.INVALID_TOKEN,
            ServerErrorCode.Auth.EXPIRED_TOKEN,
            ServerErrorCode.Auth.FORBIDDEN_REFRESH_TOKEN,
        )
    }
}
