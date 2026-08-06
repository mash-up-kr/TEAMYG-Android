package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
