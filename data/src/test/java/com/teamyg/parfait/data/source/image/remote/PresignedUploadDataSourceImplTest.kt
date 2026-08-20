package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.di.NetworkModule
import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresignedUploadDataSourceImplTest {
    private lateinit var server: MockWebServer
    private lateinit var dataSource: PresignedUploadDataSource
    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = PresignedUploadDataSourceImpl(NetworkModule.provideUploadOkHttpClient())
        file = File.createTempFile("topping", ".png")
        file.writeBytes(ByteArray(FILE_SIZE) { index -> index.toByte() })
    }

    @AfterTest
    fun tearDown() {
        server.close()
        file.delete()
    }

    @Test
    fun impl_injectsUploadQualifiedClient() {
        // Given·When 생성자에 붙은 한정자를 본다
        val qualifiers = PresignedUploadDataSourceImpl::class.java
            .declaredConstructors
            .single()
            .parameterAnnotations
            .single()
            .map { annotation -> annotation.annotationClass }

        // Then @UploadClient 다. 빠지면 공유 클라이언트가 주입돼 Authorization 이 붙고 업로드가
        // 통째로 죽는데, 이 PR 은 소비자가 0 이라 컴파일도 assembleDebug 도 그것을 못 잡는다
        assertTrue(UploadClient::class in qualifiers)
    }

    @Test
    fun put_serverAccepts_sendsPutWithGivenContentType() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 발급 때 쓴 contentType 으로 올린다
        val result = dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 성공하고 PUT 으로 그 타입이 그대로 나간다 — 발급 값과 어긋나면 S3 가 거절한다
        assertTrue(result.isSuccess)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("image/png", recorded.headers["Content-Type"])
    }

    @Test
    fun put_serverAccepts_doesNotSendAuthorizationHeader() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 올린다
        dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then Authorization 이 없다. 붙으면 S3 가 서명 수단 중복으로 거절해 업로드가 아예 안 된다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun put_serverAccepts_sendsWholeFile() = runTest {
        // Given S3 가 200 을 준다
        server.enqueue(MockResponse.Builder().code(200).build())

        // When 올린다
        dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 파일 전체가 나간다
        val recorded = server.takeRequest()
        assertEquals(file.length().toString(), recorded.headers["Content-Length"])
    }

    @Test
    fun put_serverRejects_failsWithStatusCode() = runTest {
        // Given S3 가 403 으로 거절한다(서명 불일치·만료가 이 모양으로 온다)
        server.enqueue(MockResponse.Builder().code(403).build())

        // When 올린다
        val result = dataSource.put(
            uploadUrl = server.url("/upload").toString(),
            contentType = "image/png",
            file = file,
        )

        // Then 상태 코드를 실은 실패다 — S3 거절은 서버 로그에 안 남아 이 값이 유일한 단서다
        val unknown = assertIs<ApiException.Unknown>(result.exceptionOrNull())
        val cause = assertIs<PresignedUploadException>(unknown.cause)
        assertEquals(403, cause.statusCode)
    }

    @Test
    fun put_malformedUploadUrl_failsInsteadOfThrowing() = runTest {
        // Given 서버가 준 uploadUrl 이 http/https 가 아니다

        // When 올린다
        val result = dataSource.put(
            uploadUrl = "not a url",
            contentType = "image/png",
            file = file,
        )

        // Then 예외로 새지 않고 Result 로 돌아온다. uploadUrl 은 서버가 주는 값이라 앱이 통제 못 한다
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }

    @Test
    fun put_connectionFails_failsAsNetwork() = runTest {
        // Given 아무도 듣지 않는 포트다

        // When 올린다
        val result = dataSource.put(
            uploadUrl = UNREACHABLE_URL,
            contentType = "image/png",
            file = file,
        )

        // Then 재시도가 의미 있는 갈래로 분류된다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    private companion object {
        const val FILE_SIZE = 1024

        /** 특권 포트 1 은 어떤 서버도 듣지 않아 연결이 즉시 거절된다 */
        const val UNREACHABLE_URL = "http://127.0.0.1:1/upload"
    }
}
