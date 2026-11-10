package com.teamyg.parfait.data.repository.notice

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.notice.local.NoticeLocalDataSource
import com.teamyg.parfait.data.source.notice.local.NoticeReadLocalDataSource
import com.teamyg.parfait.data.source.notice.remote.NoticeRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.notice.NoticeVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NoticeRepositoryImplTest {
    private val remote: NoticeRemoteDataSource = mockk()
    private val local: NoticeLocalDataSource = mockk(relaxed = true)
    private val readLocal: NoticeReadLocalDataSource = mockk(relaxed = true)
    private val repository get() = NoticeRepositoryImpl(remote, local, readLocal)

    private val notice = NoticeVO(
        id = 1L,
        title = "공지",
        publishedDate = LocalDate(2026, 11, 17),
        content = "본문",
    )

    @Test
    fun refreshNotices_success_savesToCache() = runTest {
        // Given 서버가 공지를 준다
        coEvery { remote.getNotices() } returns Result.success(listOf(notice))

        // When 갱신한다
        val result = repository.refreshNotices()

        // Then 캐시에 저장하고 성공이다
        assertTrue(result.isSuccess)
        verify(exactly = 1) { local.saveNotices(listOf(notice)) }
    }

    @Test
    fun refreshNotices_failure_keepsExistingCache() = runTest {
        // Given 서버 호출이 네트워크로 실패한다
        coEvery { remote.getNotices() } returns Result.failure(ApiException.Network(IOException("down")))

        // When 갱신한다
        val result = repository.refreshNotices()

        // Then 기존 캐시를 덮어쓰지 않고 실패를 전달한다
        assertTrue(result.isFailure)
        verify(exactly = 0) { local.saveNotices(any()) }
    }

    @Test
    fun refreshNotices_failure_mapsToAppError() = runTest {
        // Given 서버 호출이 네트워크로 실패한다
        coEvery { remote.getNotices() } returns Result.failure(ApiException.Network(IOException("down")))

        // When 갱신한다
        val error = repository.refreshNotices().exceptionOrNull()

        // Then 화면 쪽이 다루는 AppError 로 바뀐다
        assertIs<AppError>(error)
    }

    @Test
    fun notices_exposesLocalCacheFlow() = runTest {
        // Given 캐시에 공지가 있다 — Repository 가 만들어질 때 캐시 Flow 를 잡으므로 스텁을 먼저 건다
        every { local.notices } returns MutableStateFlow(listOf(notice))

        // Then 그 값을 그대로 구독한다
        assertEquals(listOf(notice), repository.notices.first())
    }

    @Test
    fun readNoticeIds_exposesReadStorageFlow() = runTest {
        // Given 읽음 저장소에 id 가 있다
        every { readLocal.readNoticeIds } returns flowOf(setOf(1L, 2L))

        // Then 그 값을 그대로 구독한다
        assertEquals(setOf(1L, 2L), repository.readNoticeIds.first())
    }

    @Test
    fun markRead_delegatesToReadStorage() = runTest {
        // When 읽음 처리한다
        repository.markRead(3L)

        // Then 읽음 저장소에 맡긴다
        coVerify(exactly = 1) { readLocal.markRead(3L) }
    }

    @Test
    fun clearNotices_clearsCacheButNotReadIds() = runTest {
        // When 세션이 끝나 캐시를 비운다
        repository.clearNotices()

        // Then 캐시만 비우고 읽음 기록은 건드리지 않는다 — 기기 단위로 유지한다
        verify(exactly = 1) { local.clear() }
        coVerify(exactly = 0) { readLocal.markRead(any()) }
    }
}
