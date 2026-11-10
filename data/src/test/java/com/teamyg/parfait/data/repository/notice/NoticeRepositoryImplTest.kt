package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.notice.NoticeVO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeRepositoryImplTest {
    private val noticeRemoteDataSource: NoticeRemoteDataSource = mockk()
    private val repository = NoticeRepositoryImpl(noticeRemoteDataSource)

    private val notices = listOf(
        NoticeVO(id = 2L, title = "두번째", date = "2026.11.12", content = "본문2", isNew = true),
        NoticeVO(id = 1L, title = "첫번째", date = "2026.11.01", content = "본문1", isNew = false),
    )

    @Test
    fun getNotices_dataSourceReturnsSuccess_returnsSameList() = runTest {
        // Given 원격 데이터소스가 공지를 준다
        coEvery { noticeRemoteDataSource.getNotices() } returns Result.success(notices)

        // When 목록을 조회한다
        val result = repository.getNotices()

        // Then 순서를 포함해 그대로 전달한다
        assertEquals(notices, result.getOrThrow())
    }

    @Test
    fun getNotice_existingId_returnsThatNotice() = runTest {
        // Given 목록에 id 1 이 있다
        coEvery { noticeRemoteDataSource.getNotices() } returns Result.success(notices)

        // When id 1 로 조회한다
        val notice = repository.getNotice(1L).getOrThrow()

        // Then 목록의 같은 항목이다
        assertEquals(notices[1], notice)
    }

    @Test
    fun getNotice_unknownId_returnsFailure() = runTest {
        // Given 목록에 없는 id
        coEvery { noticeRemoteDataSource.getNotices() } returns Result.success(notices)

        // When 없는 id 로 조회한다
        val result = repository.getNotice(-1L)

        // Then NoSuchElementException 으로 실패한다
        assertIs<NoSuchElementException>(result.exceptionOrNull())
    }

    @Test
    fun getNotice_listFails_returnsFailure() = runTest {
        // Given 목록 조회가 네트워크로 실패한다
        coEvery { noticeRemoteDataSource.getNotices() } returns Result.failure(IOException("down"))

        // When 상세를 조회한다
        val result = repository.getNotice(1L)

        // Then 실패가 그대로 전달된다
        assertTrue(result.isFailure)
    }
}
