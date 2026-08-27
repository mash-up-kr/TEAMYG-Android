package com.teamyg.parfait.data.network

import app.cash.turbine.test
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.model.request.auth.ReissueRequest
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSource
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasPoller
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.domain.model.session.SessionEvent
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 저장 매체를 끌어들이지 않으려고 [TokenStore] 를 메모리 페이크로 둔다.
 * 실제 [com.teamyg.parfait.data.source.token.local.EncryptedTokenStore] 는 Keystore 를
 * 요구해 JVM 단위 테스트에서 돌지 않는다.
 */
private class FakeTokenStore(
    var accessToken: String? = null,
    var refreshToken: String? = null,
) : TokenStore {
    var clearCount: Int = 0

    override suspend fun getAccessToken(): String? = accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
    ) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun clear() {
        clearCount++
        accessToken = null
        refreshToken = null
    }
}

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private lateinit var tokenStore: FakeTokenStore
    private lateinit var sessionEventBus: SessionEventBus
    private lateinit var userInfoLocalDataSource: UserInfoLocalDataSource
    private lateinit var groupLocalDataSource: GroupLocalDataSource
    private lateinit var canvasLocalDataSource: CanvasLocalDataSource
    private lateinit var canvasPoller: CanvasPoller
    private lateinit var authenticator: TokenAuthenticator

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()

        tokenStore = FakeTokenStore(accessToken = OLD_ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        sessionEventBus = SessionEventBus()
        userInfoLocalDataSource = mockk(relaxed = true)
        groupLocalDataSource = mockk(relaxed = true)
        canvasLocalDataSource = mockk(relaxed = true)
        canvasPoller = mockk(relaxed = true)

        val authService = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthService::class.java)

        authenticator = TokenAuthenticator(
            tokenStore = tokenStore,
            authService = authService,
            apiCaller = ApiCaller(json),
            sessionEventBus = sessionEventBus,
            userInfoLocalDataSource = userInfoLocalDataSource,
            groupLocalDataSource = groupLocalDataSource,
            canvasLocalDataSource = canvasLocalDataSource,
            canvasPoller = Lazy { canvasPoller },
        )
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    /** 인증이 필요한 요청이 [token] 을 달고 나갔다가 401 을 맞은 상황을 만든다 */
    private fun unauthorizedResponse(token: String?): Response {
        val request = Request
            .Builder()
            .url(server.url("/api/parfait-groups"))
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()

        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    private fun enqueueReissueSuccess() {
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body(
                    """
                    {"success":true,"code":"OK","message":"성공",
                     "data":{"accessToken":"$NEW_ACCESS_TOKEN","refreshToken":"$NEW_REFRESH_TOKEN","expiresIn":3600}}
                    """.trimIndent(),
                ).build(),
        )
    }

    @Test
    fun authenticate_reissueSucceeds_retriesWithNewToken() {
        // Given 만료된 access token 으로 나갔다가 401 을 맞았고, 재발급은 성공한다
        enqueueReissueSuccess()

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 새 토큰을 단 요청이 나오고, 저장소도 갱신된다
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retried.header("Authorization"))
        assertEquals(listOf("Bearer $NEW_ACCESS_TOKEN"), retried.headers.values("Authorization"))
        assertEquals(NEW_ACCESS_TOKEN, runBlocking { tokenStore.getAccessToken() })
        assertEquals(NEW_REFRESH_TOKEN, runBlocking { tokenStore.getRefreshToken() })
        assertEquals(0, tokenStore.clearCount)
    }

    @Test
    fun authenticate_reissueRejected_clearsTokensAndPostsForcedLogout() = runTest {
        // Given 서버가 refresh token 을 401 INVALID_TOKEN 으로 거절한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"유효하지 않은 토큰입니다","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 재시도하지 않고 세션을 버린다
        assertNull(retried)
        assertEquals(1, tokenStore.clearCount)
        sessionEventBus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    @Test
    fun authenticate_reissueRejected_clearsUserInfoTogether() = runTest {
        // Given 서버가 refresh token 을 거절한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"…","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 토큰과 함께 계정 정보도 지워진다 — 이벤트가 유실돼도 둘이 갈라지면 안 된다
        assertEquals(1, tokenStore.clearCount)
        coVerify(exactly = 1) { userInfoLocalDataSource.clear() }
        // Then 토큰·계정 정보와 함께 그룹 캐시도 지운다
        verify(exactly = 1) { groupLocalDataSource.clear() }
        // Then 오늘 캔버스 캐시도 함께 지운다 — 안 지우면 다음 계정에 이전 계정의
        // 캔버스가 보인다
        verify(exactly = 1) { canvasLocalDataSource.clear() }
    }

    @Test
    fun authenticate_reissueRejected_clearsInMemoryCachesBeforeTheAccountStore() = runTest {
        // Given 서버가 refresh token 을 거절한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"…","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 인메모리 캐시(그룹·캔버스)를 먼저 지우고 계정 저장소는 나중에 지운다.
        // 캔버스 캐시는 폴러를 먼저 세운 뒤 지운다 — 늦게 온 응답이 지운 캐시를 되살리지
        // 못하게 트리거부터 끊는다
        coVerifyOrder {
            groupLocalDataSource.clear()
            canvasPoller.stopAll()
            canvasLocalDataSource.clear()
            userInfoLocalDataSource.clear()
        }
    }

    @Test
    fun authenticate_userInfoClearThrows_stillPostsForcedLogout() = runTest {
        // Given 서버가 refresh token 을 거절하고, 계정 정보 clear() 는 DataStore IO 실패로 던진다
        server.enqueue(
            MockResponse
                .Builder()
                .code(401)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"…","data":null}""")
                .build(),
        )
        coEvery { userInfoLocalDataSource.clear() } throws IOException("disk full")

        // When 인증기가 응답을 받는다 — clear() 의 예외는 그대로 authenticate() 밖으로
        // 샌다(이 동작 자체는 이번 수정 범위 밖이다)
        assertFailsWith<IOException> {
            authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))
        }

        // Then 정리가 실패해도 이벤트는 이미 발행된 뒤라 여전히 도착한다 — 이벤트가
        // "정리 완료"가 아니라 "세션이 죽었다"를 알리기 때문이다
        sessionEventBus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
        // Then 계정 정보 clear() 가 던져도 그룹·캔버스 캐시는 이미 지워진 뒤다 — 던지지
        // 않는 정리를 먼저 하므로 뒤이은 IO 실패가 그 정리를 막지 못한다(이전 계정의
        // 그룹·캔버스가 남는 위험을 강제 로그아웃 경로에서도 막는다)
        verify(exactly = 1) { groupLocalDataSource.clear() }
        verify(exactly = 1) { canvasLocalDataSource.clear() }
    }

    @Test
    fun authenticate_reissueRejectedAsEnvelopeFailure_clearsTokensAndPostsForcedLogout() = runTest {
        // Given 서버가 HTTP 200 안에 success:false·INVALID_TOKEN 을 실어 refresh token 을 거절한다
        // (envelope 실패라 statusCode 는 null 로 오고, 판정은 code 만으로 이뤄져야 한다)
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("""{"success":false,"code":"INVALID_TOKEN","message":"유효하지 않은 토큰입니다","data":null}""")
                .build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 재시도하지 않고 세션을 버린다
        assertNull(retried)
        assertEquals(1, tokenStore.clearCount)
        sessionEventBus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    @Test
    fun authenticate_reissueForbiddenWithTokenRejectionCode_clearsTokensAndPostsForcedLogout() = runTest {
        // Given 서버가 403 + FORBIDDEN_REFRESH_TOKEN 으로 refresh token 자체를 거절한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(403)
                .body(
                    """{"success":false,"code":"FORBIDDEN_REFRESH_TOKEN","message":"다른 회원의 토큰입니다","data":null}""",
                ).build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 상태코드가 아니라 본문의 code 로 거절을 알아보고 세션을 버린다
        assertNull(retried)
        assertEquals(1, tokenStore.clearCount)
        sessionEventBus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    @Test
    fun authenticate_reissueForbiddenWithHtmlBody_keepsSession() = runTest {
        // Given WAF·사내 프록시·CDN 이 HTML 본문을 실은 403 을 돌려준다
        // (envelope 로 파싱되지 않아 code 가 없는 ApiException.Http(403) 이 된다)
        server.enqueue(
            MockResponse
                .Builder()
                .code(403)
                .body("<html><head><title>403 Forbidden</title></head><body>Forbidden</body></html>")
                .build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 인프라가 막은 것이지 자격증명이 죽은 것이 아니다 — 로그인 상태를 유지한다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        assertEquals(REFRESH_TOKEN, tokenStore.refreshToken)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_reissueNetworkFails_keepsTokensAndPostsNothing() = runTest {
        // Given 연결이 끊겨 재발급 요청 자체가 실패한다
        server.close()

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 토큰을 지우지 않는다 — 연결 실패는 자격증명이 죽은 것과 다른 사건이다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        assertEquals(REFRESH_TOKEN, tokenStore.refreshToken)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_reissueServerError_keepsTokens() = runTest {
        // Given 재발급이 500 으로 실패한다
        server.enqueue(
            MockResponse
                .Builder()
                .code(500)
                .body("{}")
                .build(),
        )

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 서버 장애로 세션을 버리지 않는다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_noRefreshToken_postsNothing() = runTest {
        // Given 로그인한 적이 없어 refresh token 이 없다
        tokenStore.accessToken = null
        tokenStore.refreshToken = null

        // When 인증기가 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(token = null))

        // Then 조용히 포기한다 — 여기서 강제 로그아웃을 쏘면 로그인 화면이 자기 자신으로 튕긴다
        assertNull(retried)
        assertEquals(0, tokenStore.clearCount)
        assertEquals(0, server.requestCount)
        sessionEventBus.events.test {
            expectNoEvents()
        }
    }

    @Test
    fun authenticate_tokenAlreadyRefreshed_retriesWithoutReissue() = runTest {
        // Given 401 두 건이 같은 낡은 토큰을 들고 있었고, 첫 건이 재발급을 끝냈다
        enqueueReissueSuccess()
        authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // When 뒤따라온 두 번째 401 이 처리된다
        val retried = authenticator.authenticate(route = null, response = unauthorizedResponse(OLD_ACCESS_TOKEN))

        // Then 재발급을 다시 쏘지 않고 이미 갱신된 토큰으로 재시도만 한다
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retried.header("Authorization"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun authenticate_retriedTwice_givesUp() = runTest {
        // Given 새 토큰으로 재시도했는데도 서버가 또 401 을 준 상황
        enqueueReissueSuccess()
        val exhausted = unauthorizedResponse(OLD_ACCESS_TOKEN)
            .newBuilder()
            .priorResponse(unauthorizedResponse(OLD_ACCESS_TOKEN))
            .build()

        // When 인증기가 그 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = exhausted)

        // Then 재발급조차 시도하지 않고 포기한다 — 무한 재시도를 끊는다
        assertNull(retried)
        assertEquals(0, server.requestCount)
    }

    /** 로드밸런서의 http→https 이동 같은, 401 이 아닌 선행 응답 */
    private fun movedPermanentlyResponse(): Response = Response
        .Builder()
        .request(Request.Builder().url(server.url("/api/parfait-groups")).build())
        .protocol(Protocol.HTTP_1_1)
        .code(301)
        .message("Moved Permanently")
        .build()

    @Test
    fun authenticate_priorResponseIsRedirect_stillReissues() = runTest {
        // Given 리다이렉트를 한 번 거친 뒤 처음으로 401 을 맞았다 —
        // `priorResponse` 체인에 401 이 아닌 응답이 섞여 있다
        enqueueReissueSuccess()
        val afterRedirect = unauthorizedResponse(OLD_ACCESS_TOKEN)
            .newBuilder()
            .priorResponse(movedPermanentlyResponse())
            .build()

        // When 인증기가 그 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = afterRedirect)

        // Then 리다이렉트는 재시도 횟수가 아니다 — 첫 401 이므로 재발급을 시도한다
        assertNotNull(retried)
        assertEquals("Bearer $NEW_ACCESS_TOKEN", retried.header("Authorization"))
        assertEquals(1, server.requestCount)
    }

    /**
     * [AuthService.postAuthReissue] 가 실제로 만드는, `@NoAuth` 태그가 달린 [Request] 를
     * 포착한다. 인터셉터가 네트워크로 나가기 전에 바로 가짜 200 을 돌려주므로
     * `server.requestCount` 는 이 과정에서 늘지 않는다 — 이 테스트가 확인하려는 것은
     * "재발급 요청 자신이 401 을 받아도 인증기가 재진입하지 않는다"이지 실제 네트워크
     * 왕복이 아니다.
     */
    private fun capturedReissueRequest(): Request {
        var captured: Request? = null
        val capturingClient = OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                captured = chain.request()
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        {"success":true,"code":"OK","message":"성공",
                         "data":{"accessToken":"$NEW_ACCESS_TOKEN","refreshToken":"$NEW_REFRESH_TOKEN","expiresIn":3600}}
                        """.trimIndent().toResponseBody("application/json".toMediaType()),
                    ).build()
            }.build()

        val capturingAuthService = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(capturingClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthService::class.java)

        runBlocking { capturingAuthService.postAuthReissue(ReissueRequest(refreshToken = REFRESH_TOKEN)) }
        return requireNotNull(captured)
    }

    @Test
    fun authenticate_noAuthEndpointGets401_returnsNullWithoutReissue() = runTest {
        // Given 재발급 요청(`@NoAuth`) 자신이 401 을 받은 상황 — 같은 OkHttpClient 를 타므로
        // 이 인증기가 그 요청도 다시 붙잡을 수 있다(재진입 데드락 경로)
        val reissueRequest = capturedReissueRequest()
        val unauthorizedReissue = Response
            .Builder()
            .request(reissueRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        // When 인증기가 그 응답을 받는다
        val retried = authenticator.authenticate(route = null, response = unauthorizedReissue)

        // Then 재발급을 시도조차 하지 않고 즉시 포기한다
        assertNull(retried)
        assertEquals(0, server.requestCount)
    }

    private companion object {
        const val OLD_ACCESS_TOKEN = "old-access"
        const val NEW_ACCESS_TOKEN = "new-access"
        const val REFRESH_TOKEN = "refresh"
        const val NEW_REFRESH_TOKEN = "new-refresh"
    }
}
