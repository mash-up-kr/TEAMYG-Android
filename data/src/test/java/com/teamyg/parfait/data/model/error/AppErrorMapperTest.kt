package com.teamyg.parfait.data.model.error

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.AppError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class AppErrorMapperTest {
    // `ApiCallerTest` 의 기존 헬퍼와 같은 방식이다
    private fun httpException(statusCode: Int): HttpException {
        val responseBody = "".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(statusCode, responseBody))
    }

    @Test
    fun toAppError_business_mapsToServerWithCodeAndStatus() {
        // Given 서버가 에러 envelope 를 준 상황
        val exception = ApiException.Business(
            code = "INVALID_ID_TOKEN",
            serverMessage = "유효하지 않은 ID 토큰입니다",
            statusCode = 401,
            errorDetail = null,
        )

        // When 도메인 에러로 변환
        val error = exception.toAppError()

        // Then code·statusCode·메시지가 그대로 실린다
        val server = assertIs<AppError.Server>(error)
        assertEquals("INVALID_ID_TOKEN", server.code)
        assertEquals(401, server.statusCode)
        assertEquals("유효하지 않은 ID 토큰입니다", server.serverMessage)
    }

    @Test
    fun toAppError_network_mapsToNetworkKeepingCause() {
        // Given 연결 실패
        val cause = IOException("connection reset")
        val exception = ApiException.Network(cause)

        // When 도메인 에러로 변환
        val error = exception.toAppError()

        // Then Network 갈래이고 원인이 보존된다
        val network = assertIs<AppError.Network>(error)
        assertSame(cause, network.cause)
    }

    @Test
    fun toAppError_httpAndEmptyBodyAndUnknown_mapToUnexpected() {
        // Given envelope 밖 HTTP 실패·빈 본문·정체불명 예외
        val http = ApiException.Http(statusCode = 500, cause = httpException(500))
        val emptyBody = ApiException.EmptyBody(code = "OK", serverMessage = "본문 없음")
        val unknown = ApiException.Unknown(IllegalStateException("boom"))

        // When 각각 변환
        // Then 셋 다 Unexpected 로 접힌다
        assertIs<AppError.Unexpected>(http.toAppError())
        assertIs<AppError.Unexpected>(emptyBody.toAppError())
        assertIs<AppError.Unexpected>(unknown.toAppError())
    }

    @Test
    fun toAppError_cancellation_rethrows() {
        // Given 취소 예외
        val cancellation = CancellationException("cancelled")

        // When·Then 변환하지 않고 그대로 다시 던진다
        assertFailsWith<CancellationException> { cancellation.toAppError() }
    }

    @Test
    fun mapErrorToAppError_failure_replacesThrowableWithAppError() {
        // Given ApiException 을 실은 실패 Result
        val result: Result<String> = Result.failure(
            ApiException.Business(
                code = "KAKAO_SERVER_UNAVAILABLE",
                serverMessage = "카카오 서버에 연결할 수 없습니다",
                statusCode = 503,
                errorDetail = null,
            ),
        )

        // When 도메인 에러로 갈아끼운다
        val mapped = result.mapErrorToAppError()

        // Then 실패 원인이 AppError.Server 다
        val server = assertIs<AppError.Server>(mapped.exceptionOrNull())
        assertEquals("KAKAO_SERVER_UNAVAILABLE", server.code)
    }

    @Test
    fun mapErrorToAppError_success_passesValueThrough() {
        // Given 성공 Result
        val result = Result.success("ok")

        // When 변환
        val mapped = result.mapErrorToAppError()

        // Then 값이 그대로다
        assertEquals("ok", mapped.getOrNull())
    }
}
