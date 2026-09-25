package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.qualifier.UnauthenticatedClient
import com.teamyg.parfait.data.network.NetworkConstValue.AUTHORIZATION_HEADER
import com.teamyg.parfait.data.network.NetworkConstValue.BEARER_PREFIX
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.event.SessionEventBusImpl
import com.teamyg.parfait.data.source.auth.mapper.toAuthSessionVO
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSource
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasPoller
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.auth.AuthSessionVO
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import dagger.Lazy
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
 * [authService] 는 `@UnauthenticatedClient` 라 인증기 없는 전용 `OkHttpClient` 를 탄다. 자기 `Dispatcher` 를
 * 가져 `authenticate()` 가 점유한 슬롯과 경합하지 않고, 재발급 자신의 401 이 이 인증기를 재진입시키지 않는다.
 *
 * [canvasPoller] 를 [Lazy] 로 받는 이유: `TokenAuthenticator → CanvasPoller → ParfaitRemoteDataSource →
 * 인증 OkHttpClient → TokenAuthenticator` 순환이 있다.
 *
 * `runBlocking` 은 [Authenticator] 계약이 동기라서 쓴다.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @UnauthenticatedClient private val authService: AuthService,
    private val apiCaller: ApiCaller,
    private val sessionEventBus: SessionEventBusImpl,
    private val userInfoLocalDataSource: UserInfoLocalDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
    private val canvasLocalDataSource: CanvasLocalDataSource,
    private val canvasPoller: Lazy<CanvasPoller>,
) : Authenticator {
    private val mutex = Mutex()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // `@NoAuth` 엔드포인트는 재발급 대상이 아니다
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
                // 기다리는 동안 다른 요청이 이미 갱신했다면 새 토큰만 달아준다. 이 확인이 없으면
                // 대기하던 요청들이 차례로 각자 재발급을 쏜다
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
     * 이 응답이 몇 번째 401 인가. `priorResponse` 체인의 401 개수 + 자기 자신.
     *
     * 401 만 센다. 리다이렉트까지 세면 http→https 301 한 번만으로 첫 401 이 2회차로 보여 재발급을
     * 아예 시도하지 않는다.
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
                // 이벤트를 정리보다 먼저 쏜다. 정리의 IO 예외가 이벤트 발행까지 막으면 토큰은
                // 지워졌는데 화면은 세션이 죽은 줄 모른다
                sessionEventBus.postForcedLogout()
                // 계정 정보·캐시도 여기서 함께 지운다. 화면이 이벤트를 받아 지우게 하면 이벤트
                // 유실 시 토큰 없이 계정 정보만 남는다
                tokenStore.clear()
                // 던지지 않는 인메모리 캐시부터 지운다. 뒤의 DataStore IO 실패가 이 정리까지
                // 막지 않게 한다
                groupLocalDataSource.clear()
                // 폴러부터 세운다. 순서가 반대면 이미 출발한 응답이 빈 캐시를 되살린다
                canvasPoller.get().stopAll()
                canvasLocalDataSource.clear()
                userInfoLocalDataSource.clear()
            } else {
                // 연결 실패·서버 장애로 refresh token 을 버리지 않는다. 원요청은 401 그대로 화면에 간다
                sourceLogger.e(throwable) { "재발급 실패 — 세션 유지" }
            }
            null
        }

    /**
     * 서버가 refresh token 자체를 거절했는가. envelope 실패는 `statusCode` 가 비어 올 수 있고,
     * HTTP 실패는 `code` 없이 status 만 와서 둘을 함께 본다.
     *
     * 상태코드만으로 세션을 버리는 것은 401 뿐이다. WAF·프록시·CDN 은 HTML 403 을 흔히 돌려주므로,
     * 403 은 본문 `code` 가 토큰 거절 코드일 때만 세션을 끝낸다.
     */
    private fun Throwable.isSessionDead(): Boolean = when (this) {
        is ApiException.Business -> statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || code in SESSION_DEAD_CODES
        is ApiException.Http -> statusCode == HttpURLConnection.HTTP_UNAUTHORIZED
        else -> false
    }

    private fun Request.withBearerToken(accessToken: String): Request = newBuilder()
        .bearerAuth(accessToken)
        .build()

    private companion object {
        const val MAX_RETRY = 2

        val SESSION_DEAD_CODES = setOf(
            ServerErrorCode.Auth.INVALID_TOKEN,
            ServerErrorCode.Auth.EXPIRED_TOKEN,
            ServerErrorCode.Auth.FORBIDDEN_REFRESH_TOKEN,
        )
    }
}
