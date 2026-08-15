package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
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
            sourceLogger.e(throwable) { "토큰 재발급 실패" }
            null
        }

    private fun Request.withToken(accessToken: String): Request = newBuilder()
        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
        .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
