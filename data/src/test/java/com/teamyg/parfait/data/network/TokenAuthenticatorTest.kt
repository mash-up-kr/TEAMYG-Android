package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.data.source.token.local.TokenStore
import kotlinx.coroutines.runBlocking
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

    private companion object {
        const val OLD_ACCESS_TOKEN = "old-access"
        const val NEW_ACCESS_TOKEN = "new-access"
        const val REFRESH_TOKEN = "refresh"
        const val NEW_REFRESH_TOKEN = "new-refresh"
    }
}
