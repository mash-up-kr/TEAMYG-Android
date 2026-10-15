package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.Headers
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
import kotlin.test.assertTrue

class ApiCallerTest {
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })

    private fun success(data: String?) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = data,
    )

    private fun httpException(
        code: Int,
        body: String,
    ): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        val response = Response.error<Unit>(code, responseBody)
        return HttpException(response)
    }

    @Test
    fun safeApiCall_successWithData_returnsData() = runTest {
        // Given 데이터가 담긴 성공 응답
        val response = success("payload")

        // When safeApiCall
        val result = apiCaller.safeApiCall { response }

        // Then 데이터를 그대로 돌려준다
        assertEquals("payload", result.getOrThrow())
    }

    @Test
    fun safeApiCall_successWithTransform_appliesTransform() = runTest {
        // Given 데이터가 담긴 성공 응답
        val response = success("payload")

        // When 변환 함수와 함께 호출
        val result = apiCaller.safeApiCall(
            block = { response },
            transform = { it.length },
        )

        // Then 변환 결과를 돌려준다
        assertEquals(7, result.getOrThrow())
    }

    @Test
    fun safeApiCall_successWithNullData_returnsEmptyBody() = runTest {
        // Given success=true 인데 data 가 null
        val response = success(null)

        // When safeApiCall
        val result = apiCaller.safeApiCall { response }

        // Then EmptyBody 로 실패한다
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun safeApiCall_businessFailure_returnsBusinessWithNullStatusCode() = runTest {
        // Given success=false 인 응답
        val response = ApiResponse(
            success = false,
            code = "INVALID_NAME",
            message = "이름이 올바르지 않습니다",
            data = null,
            errorDetail = mapOf("field" to "name"),
        )

        // When safeApiCall
        val result = apiCaller.safeApiCall { response }

        // Then Business 예외. HTTP 계층을 거치지 않았으므로 statusCode 는 null 이다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_NAME", error.code)
        assertEquals(null, error.statusCode)
        assertEquals(mapOf("field" to "name"), error.errorDetail)
    }

    @Test
    fun safeApiCall_ioException_returnsNetwork() = runTest {
        // Given 네트워크 예외
        val exception = IOException("timeout")

        // When safeApiCall
        val result = apiCaller.safeApiCall<String> { throw exception }

        // Then Network 로 감싼다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun safeApiCallWithoutData_success_returnsUnit() = runTest {
        // Given 본문 없는 성공 응답
        val response = ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = Unit,
        )

        // When safeApiCallWithoutData
        val result = apiCaller.safeApiCallWithoutData { response }

        // Then data 가 없어도 성공이다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun safeApiCallNoContent_blockSucceeds_returnsSuccess() = runTest {
        // Given 응답 본문 자체가 없는 호출
        val block: suspend () -> Unit = { }

        // When safeApiCallNoContent
        val result = apiCaller.safeApiCallNoContent(block)

        // Then 성공
        assertTrue(result.isSuccess)
    }

    @Test
    fun safeApiCallNoContent_blockThrowsIo_returnsNetwork() = runTest {
        // Given 블록이 IO 예외를 던진다
        val exception = IOException("reset")

        // When safeApiCallNoContent
        val result = apiCaller.safeApiCallNoContent { throw exception }

        // Then Network 로 감싼다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun safeApiCall_httpExceptionWithBlankErrorBody_returnsHttp() = runTest {
        // Given 에러 바디가 비어있는 HttpException
        val exception = httpException(code = 500, body = "")

        // When safeApiCall
        val result = apiCaller.safeApiCall<String> { throw exception }

        // Then 봉투 파싱을 시도하지 않고 Http 로 감싼다
        val error = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(500, error.statusCode)
    }

    @Test
    fun safeApiCall_httpExceptionWithUnparsableErrorBody_returnsHttp() = runTest {
        // Given 에러 바디는 있지만 ApiResponse 봉투 형식이 아니다
        val exception = httpException(code = 502, body = "not json")

        // When safeApiCall
        val result = apiCaller.safeApiCall<String> { throw exception }

        // Then 봉투 파싱에 실패하면 Http 로 감싼다
        val error = assertIs<ApiException.Http>(result.exceptionOrNull())
        assertEquals(502, error.statusCode)
    }

    @Test
    fun safeApiCall_httpExceptionWithEnvelopeErrorBody_returnsBusinessWithStatusCode() = runTest {
        // Given 에러 바디가 ApiResponse 봉투로 파싱된다
        val body = """
            {"success":false,"code":"BUSINESS_ERROR","message":"업무 오류","data":null,"errorDetail":{"field":"name"}}
        """.trimIndent()
        val exception = httpException(code = 400, body = body)

        // When safeApiCall
        val result = apiCaller.safeApiCall<String> { throw exception }

        // Then Business 로 감싸고, HTTP 계층에서 온 것이므로 statusCode 를 함께 담는다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("BUSINESS_ERROR", error.code)
        assertEquals(400, error.statusCode)
        assertEquals(mapOf("field" to "name"), error.errorDetail)
    }

    @Test
    fun safeApiCall_blockThrowsCancellation_propagatesInsteadOfWrapping() = runTest {
        // Given 코루틴 취소 예외
        val exception = CancellationException("cancelled")

        // When / Then Result 로 감싸지 않고 그대로 던진다 — catch 절 순서가
        // Exception 보다 먼저여야 하는 이유를 지키는 회귀 방어다
        assertFailsWith<CancellationException> {
            apiCaller.safeApiCall<String> { throw exception }
        }
    }

    @Test
    fun safeApiCallNoContent_blockThrowsCancellation_propagatesInsteadOfWrapping() = runTest {
        // Given 코루틴 취소 예외
        val exception = CancellationException("cancelled")

        // When / Then Result 로 감싸지 않고 그대로 던진다
        assertFailsWith<CancellationException> {
            apiCaller.safeApiCallNoContent { throw exception }
        }
    }

    @Test
    fun safeConditionalApiCall_304_returnsTheNotModifiedValue() = runTest {
        // Given 본문 없는 304
        val response = notModifiedResponse<ApiResponse<String>>()

        // When 조건부 호출
        val result = apiCaller.safeConditionalApiCall(
            block = { response },
            notModified = "NOT_MODIFIED",
            transform = { data, _ -> data },
        )

        // Then 304 를 에러가 아니라 성공 값으로 돌려준다
        assertEquals("NOT_MODIFIED", result.getOrThrow())
    }

    @Test
    fun safeConditionalApiCall_200_handsTheDataAndHeadersToTransform() = runTest {
        // Given ETag 헤더가 붙은 200
        val response = Response.success(
            success("payload"),
            Headers.headersOf("ETag", "W/\"v1\""),
        )

        // When 조건부 호출
        val result = apiCaller.safeConditionalApiCall(
            block = { response },
            notModified = "NOT_MODIFIED",
            transform = { data, headers -> "$data|${headers["ETag"]}" },
        )

        // Then 본문과 헤더 값이 함께 변환에 들어온다
        assertEquals("payload|W/\"v1\"", result.getOrThrow())
    }

    @Test
    fun safeConditionalApiCall_errorStatus_isConvertedLikeOtherCalls() = runTest {
        // Given 비즈니스 에러 본문이 실린 404
        val body = """{"success":false,"code":"PARFAIT_NOT_FOUND","message":"없음"}"""
        val response = Response.error<ApiResponse<String>>(
            404,
            body.toResponseBody("application/json".toMediaType()),
        )

        // When 조건부 호출
        val result = apiCaller.safeConditionalApiCall(
            block = { response },
            notModified = "NOT_MODIFIED",
            transform = { data, _ -> data },
        )

        // Then 다른 호출과 같이 Business 로 바뀌고 HTTP status 를 담는다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("PARFAIT_NOT_FOUND", error.code)
        assertEquals(404, error.statusCode)
    }

    @Test
    fun safeConditionalApiCall_blockThrowsCancellation_propagatesInsteadOfWrapping() = runTest {
        // Given 코루틴 취소 예외
        val exception = CancellationException("cancelled")

        // When / Then Result 로 감싸지 않고 그대로 던진다
        assertFailsWith<CancellationException> {
            apiCaller.safeConditionalApiCall<String, String>(
                block = { throw exception },
                notModified = "NOT_MODIFIED",
                transform = { data, _ -> data },
            )
        }
    }

    @Test
    fun safeConditionalApiCall_successFalseOn200_returnsBusiness() = runTest {
        // Given success=false 인 200
        val response = Response.success(ApiResponse<String>(success = false, code = "FAIL", message = "실패"))

        // When 조건부 호출
        val result = apiCaller.safeConditionalApiCall(
            block = { response },
            notModified = "NOT_MODIFIED",
            transform = { data, _ -> data },
        )

        // Then 본문의 success 를 믿지 않고 Business 실패다
        assertEquals("FAIL", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }

    @Test
    fun safeConditionalApiCall_2xxWithoutBody_returnsUnknown() = runTest {
        // Given 본문이 없는 200
        val response = Response.success<ApiResponse<String>>(null)

        // When 조건부 호출
        val result = apiCaller.safeConditionalApiCall(
            block = { response },
            notModified = "NOT_MODIFIED",
            transform = { data, _ -> data },
        )

        // Then NPE 로 터지지 않고 실패 값으로 돌아온다
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }
}
