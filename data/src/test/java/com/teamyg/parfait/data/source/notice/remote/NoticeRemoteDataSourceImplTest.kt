package com.teamyg.parfait.data.source.notice.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.NoticeService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.notice.NoticeResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeRemoteDataSourceImplTest {
    private val noticeService: NoticeService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = NoticeRemoteDataSourceImpl(
        noticeService = noticeService,
        apiCaller = apiCaller,
    )

    private fun response(vararg ids: Long) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ids.map { id ->
            NoticeResponse(
                id = id,
                type = "UPDATE",
                title = "공지 $id",
                content = "본문 $id",
                publishedDate = "2026-11-1$id",
            )
        },
    )

    @Test
    fun getNotices_success_returnsVoListInServerOrder() = runTest {
        // Given 서버가 최신순으로 2, 1 을 준다
        coEvery { noticeService.getNotices("ANDROID") } returns response(2L, 1L)

        // When 목록을 조회한다
        val notices = dataSource.getNotices().getOrThrow()

        // Then 순서를 바꾸지 않고 VO 로 매핑한다
        assertEquals(listOf(2L, 1L), notices.map { it.id })
    }

    @Test
    fun getNotices_always_requestsAndroidPlatformOnce() = runTest {
        // Given 빈 목록 응답
        coEvery { noticeService.getNotices(any()) } returns response()

        // When 목록을 조회한다
        dataSource.getNotices()

        // Then 플랫폼 ANDROID 로 한 번만 호출한다
        coVerify(exactly = 1) { noticeService.getNotices("ANDROID") }
    }

    @Test
    fun getNotices_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 success=false 로 응답한다
        coEvery { noticeService.getNotices(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_REQUEST",
            message = "잘못된 요청",
            data = null,
        )

        // When 목록을 조회한다
        val result = dataSource.getNotices()

        // Then Business 예외로 실패한다
        assertIs<ApiException.Business>(result.exceptionOrNull())
    }

    @Test
    fun getNotices_ioException_returnsNetworkException() = runTest {
        // Given 네트워크가 끊겼다
        coEvery { noticeService.getNotices(any()) } throws IOException("reset")

        // When 목록을 조회한다
        val result = dataSource.getNotices()

        // Then Network 예외로 감싼다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
}
