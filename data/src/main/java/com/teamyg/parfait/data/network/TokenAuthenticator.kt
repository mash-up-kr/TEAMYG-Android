package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.qualifier.UnauthenticatedClient
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
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
import retrofit2.Invocation
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 을 가로채 access token 을 재발급하고 원요청을 다시 만든다.
 *
 * [authService] 가 `@UnauthenticatedClient` 로 한정된 이유: 재발급은 **인증기가 달리지 않은
 * `OkHttpClient`** 를 탄다(`NetworkModule.provideUnauthenticatedOkHttpClient`). 그 클라이언트는 자기
 * `Dispatcher` 를 가져 `authenticate()` 가 점유한 메인 디스패처 슬롯과 경합하지 않고,
 * 인증기가 없으므로 재발급 자신의 401 이 이 인증기를 재진입시키지도 않는다.
 * 이 분리 덕분에 `Retrofit → OkHttpClient → Authenticator → AuthService` 순환도 사라져
 * `Provider` 지연 주입이 필요 없다.
 *
 * `runBlocking` 을 쓰는 이유: [Authenticator] 계약이 동기다. 저장소 읽기·재발급이 모두
 * `suspend` 라 다른 길이 없다 — `TokenStoreTokenProvider` 도 같은 이유로 같은 방식이다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @UnauthenticatedClient private val authService: AuthService,
    private val apiCaller: ApiCaller,
    private val sessionEventBus: SessionEventBus,
    private val userInfoLocalDataSource: UserInfoLocalDataSource,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // `@NoAuth` 엔드포인트는 인증이 필요 없어 재발급 대상이 아니다.
        // 재발급 요청 자신(`postAuthReissue`)의 재진입은 이제 전용 클라이언트가 구조적으로
        // 막는다(그쪽엔 인증기가 아예 없다) — 이 가드는 그 위에 겹치는 방어선이다.
        // 인증이 필요 없는 요청까지 재발급을 태울 이유가 없다는 것 자체로도 옳다.
        val skipAuth = response.request
            .tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoAuth::class.java) == true
        if (skipAuth) return null

        // 새 토큰으로 재시도했는데 또 401 이면 재발급으로 풀릴 문제가 아니다
        if (response.retryCount() >= MAX_RETRY) {
            sourceLogger.e { "재발급 후에도 401 — 재시도를 끊는다" }
            return null
        }

        val failedToken = response.request
            .header(AUTHORIZATION_HEADER)
            ?.removePrefix(BEARER_PREFIX)

        return runBlocking {
            mutex.withLock {
                // 기다리는 동안 다른 요청이 이미 갱신했다면 재발급 없이 새 토큰만 달아준다.
                // 이 확인이 없으면 Mutex 는 직렬화만 할 뿐, 대기하던 요청들이 차례로 각자
                // 재발급을 쏜다.
                val currentToken = tokenStore.getAccessToken()
                if (currentToken != null && currentToken != failedToken) {
                    return@withLock response.request.withBearerToken(currentToken)
                }

                val refreshToken = tokenStore.getRefreshToken() ?: return@withLock null

                val session = reissue(refreshToken) ?: return@withLock null
                tokenStore.save(
                    accessToken = session.accessToken.value,
                    refreshToken = session.refreshToken.value,
                )
                response.request.withBearerToken(session.accessToken.value)
            }
        }
    }

    /**
     * 이 응답이 몇 번째 **401** 인가. `priorResponse` 체인의 401 개수 + 자기 자신(항상 401).
     *
     * 체인에는 리다이렉트 같은 401 아닌 후속 응답도 섞인다. 그것까지 세면 로드밸런서의
     * http→https 301 한 번만으로 첫 401 이 이미 2회차로 보여 재발급을 아예 시도하지 않고,
     * 인증이 필요한 모든 호출이 회복 경로 없이 실패한다.
     */
    private fun Response.retryCount(): Int {
        var count = 1
        var prior = priorResponse
        while (prior != null) {
            if (prior.code == HttpURLConnection.HTTP_UNAUTHORIZED) count++
            prior = prior.priorResponse
        }
        return count
    }

    private suspend fun reissue(refreshToken: String): AuthSessionVO? = apiCaller
        .safeApiCall(
            block = { authService.postAuthReissue(ReissueRequest(refreshToken = refreshToken)) },
            transform = { it.toAuthSessionVO() },
        ).getOrElse { throwable ->
            if (throwable.isSessionDead()) {
                sourceLogger.e(throwable) { "재발급 거절 — 세션 종료" }
                // 토큰을 지우는 이 자리에서 계정 정보도 함께 지운다. `SessionEventBus` 를
                // 구독해 화면 쪽에서 지우게 하면 이벤트가 유실될 때 토큰은 없는데 계정
                // 정보만 남는 상태가 생긴다 — `:data` 안에서 끝내면 그 경로 자체가 없다.
                tokenStore.clear()
                userInfoLocalDataSource.clear()
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
     *
     * **맨 상태코드만으로 세션을 버리는 것은 401 뿐이다.** reissue 계약상 토큰 거절은 401 로만
     * 오고, 403 `FORBIDDEN_REFRESH_TOKEN` 은 logout 소속이다. 반대로 WAF·사내 프록시·CDN 은
     * HTML 본문을 실은 403 을 흔히 돌려주는데(그러면 envelope 파싱에 실패해 [ApiException.Http]
     * 가 된다) 그것으로 로그인 상태를 유지했어야 할 사용자를 쫓아내면 안 된다. 403 은 본문의
     * `code` 가 토큰 거절 코드일 때만 세션을 끝낸다.
     */
    private fun Throwable.isSessionDead(): Boolean = when (this) {
        is ApiException.Business -> statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || code in SESSION_DEAD_CODES
        is ApiException.Http -> statusCode == HttpURLConnection.HTTP_UNAUTHORIZED
        else -> false
    }

    private fun Request.withBearerToken(accessToken: String): Request = newBuilder()
        .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$accessToken")
        .build()

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_RETRY = 2

        val SESSION_DEAD_CODES = setOf(
            ServerErrorCode.Auth.INVALID_TOKEN,
            ServerErrorCode.Auth.EXPIRED_TOKEN,
            ServerErrorCode.Auth.FORBIDDEN_REFRESH_TOKEN,
        )
    }
}
