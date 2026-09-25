package com.teamyg.parfait.data.model.mapper.exception

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.AppError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AppErrorMapperTest {
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
    fun toAppError_cancellation_rethrows() {
        // Given 취소 예외
        val cancellation = CancellationException("cancelled")

        // When·Then 변환하지 않고 그대로 다시 던진다
        assertFailsWith<CancellationException> { cancellation.toAppError() }
    }
}
