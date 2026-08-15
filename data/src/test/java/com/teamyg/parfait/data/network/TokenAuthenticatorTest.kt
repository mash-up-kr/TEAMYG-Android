package com.teamyg.parfait.data.network

import app.cash.turbine.test
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.domain.model.session.SessionEvent
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
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    private lateinit var authenticator: TokenAuthenticator

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()

        tokenStore = FakeTokenStore(accessToken = OLD_ACCESS_TOKEN, refreshToken = REFRESH_TOKEN)
        sessionEventBus = SessionEventBus()

        val authService = Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthService::class.java)

        authenticator = TokenAuthenticator(
            tokenStore = tokenStore,
            authService = Provider { authService },
            apiCaller = ApiCaller(json),
            sessionEventBus = sessionEventBus,
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

    private companion object {
        const val OLD_ACCESS_TOKEN = "old-access"
        const val NEW_ACCESS_TOKEN = "new-access"
        const val REFRESH_TOKEN = "refresh"
        const val NEW_REFRESH_TOKEN = "new-refresh"
    }
}
