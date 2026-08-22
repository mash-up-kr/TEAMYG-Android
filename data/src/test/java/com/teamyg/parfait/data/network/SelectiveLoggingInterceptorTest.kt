package com.teamyg.parfait.data.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SECRET_BODY = """{"uploadUrl":"https://s3.example.com/o?X-Amz-Signature=deadbeef"}"""

/**
 * 반환 타입이 `ResponseBody` 라 Retrofit 내장 컨버터로 읽힌다 — 이 테스트가 보는 것은 로깅
 * 선택이지 직렬화가 아니라서 컨버터 팩토리를 달지 않는다.
 */
private interface FakeService {
    @GET("plain")
    suspend fun plain(): ResponseBody

    @NoBodyLog
    @GET("secret")
    suspend fun secret(): ResponseBody
}

class SelectiveLoggingInterceptorTest {
    private lateinit var server: MockWebServer
    private val logs = mutableListOf<String>()

    private fun service(): FakeService {
        val full = HttpLoggingInterceptor { message -> logs += message }
            .apply { level = HttpLoggingInterceptor.Level.BODY }
        val redacted = HttpLoggingInterceptor { message -> logs += message }
            .apply { level = HttpLoggingInterceptor.Level.HEADERS }

        val client = OkHttpClient
            .Builder()
            .addInterceptor(SelectiveLoggingInterceptor(full = full, redacted = redacted))
            .build()

        return Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(FakeService::class.java)
    }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        logs.clear()
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun intercept_annotatedEndpoint_doesNotLogTheResponseBody() = runTest {
        // Given 응답 본문에 presigned URL 이 실려 오는 엔드포인트
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body(SECRET_BODY)
                .build(),
        )

        service().secret().close()

        // Then 그 URL 이 로그 어디에도 남지 않는다 — URL 자체가 업로드 자격증명이다
        assertFalse(logs.any { it.contains("X-Amz-Signature") })
    }

    @Test
    fun intercept_plainEndpoint_stillLogsTheResponseBody() = runTest {
        // 표시 없는 엔드포인트의 본문 로깅은 그대로여야 한다 — 전체를 낮추는 수정이 아니다
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body(SECRET_BODY)
                .build(),
        )

        service().plain().close()

        assertTrue(logs.any { it.contains("X-Amz-Signature") })
    }
}
