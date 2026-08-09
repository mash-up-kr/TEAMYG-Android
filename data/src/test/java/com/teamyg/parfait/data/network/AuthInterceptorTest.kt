package com.teamyg.parfait.data.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 테스트 전용 API. 프로덕션 Service 를 쓰지 않는 이유는 인증이 필요한
 * 엔드포인트와 [NoAuth] 엔드포인트를 한 인터페이스에서 나란히 두고 비교해야 하기
 * 때문이다. 반환 타입이 [ResponseBody] 인 건 Retrofit 내장 컨버터만으로 동작해
 * 직렬화 설정을 끌어들이지 않기 위해서다.
 */
private interface TestApi {
    @GET("authed")
    suspend fun authed(): ResponseBody

    @NoAuth
    @GET("open")
    suspend fun open(): ResponseBody
}

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    private fun createApi(token: String?): TestApi {
        val tokenProvider = object : TokenProvider {
            override fun getToken(): String? = token
        }
        val client = OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        return Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(TestApi::class.java)
    }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun intercept_tokenPresentAndEndpointRequiresAuth_addsBearerHeader() = runTest {
        // Given 토큰이 있고 인증이 필요한 엔드포인트
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("{}")
                .build(),
        )
        val api = createApi(token = "abc123")

        // When 호출
        api.authed()

        // Then Authorization 헤더가 붙는다
        val recorded = server.takeRequest()
        assertEquals("Bearer abc123", recorded.headers["Authorization"])
    }

    @Test
    fun intercept_endpointAnnotatedNoAuth_omitsHeader() = runTest {
        // Given 토큰이 있어도 @NoAuth 가 붙은 엔드포인트
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("{}")
                .build(),
        )
        val api = createApi(token = "abc123")

        // When 호출
        api.open()

        // Then Authorization 헤더를 붙이지 않는다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun intercept_tokenAbsent_omitsHeader() = runTest {
        // Given 토큰이 없다 (미로그인)
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("{}")
                .build(),
        )
        val api = createApi(token = null)

        // When 인증이 필요한 엔드포인트 호출
        api.authed()

        // Then 헤더 없이 나간다 — 빈 Bearer 를 보내지 않는다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun intercept_anyRequest_preservesPathAndMethod() = runTest {
        // Given 임의 호출
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("{}")
                .build(),
        )
        val api = createApi(token = "abc123")

        // When 호출
        api.authed()

        // Then 경로·메서드를 바꾸지 않는다
        val recorded = server.takeRequest()
        assertEquals("/authed", recorded.url.encodedPath)
        assertEquals("GET", recorded.method)
    }
}
