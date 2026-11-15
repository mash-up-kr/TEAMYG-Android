package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.usecase.notice.GetNoticeItemsUseCase
import com.teamyg.parfait.domain.usecase.notice.MarkNoticeReadUseCase
import com.teamyg.parfait.domain.usecase.notice.RefreshNoticesUseCase
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoticeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNoticeItems: GetNoticeItemsUseCase = mockk()
    private val refreshNotices: RefreshNoticesUseCase = mockk()
    private val markNoticeRead: MarkNoticeReadUseCase = mockk()

    private val key = NavKeyNoticeDetail(noticeId = 7L)

    private fun notice(id: Long) = NoticeVO(
        id = id,
        title = "공지 $id",
        publishedDate = LocalDate(2026, 11, 12),
        content = "본문 $id",
    )

    private fun item(id: Long) = NoticeItemVO(notice = notice(id), isNew = true)

    private fun viewModel(
        items: Flow<List<NoticeItemVO>?>,
        refresh: Result<Unit> = Result.success(Unit),
    ): NoticeDetailViewModel {
        every { getNoticeItems() } returns items
        coEvery { refreshNotices() } returns refresh
        coEvery { markNoticeRead(any()) } returns Unit
        return NoticeDetailViewModel(
            key = key,
            getNoticeItems = getNoticeItems,
            refreshNotices = refreshNotices,
            markNoticeRead = markNoticeRead,
        )
    }

    @Test
    fun init_beforeCacheEmits_isLoadingWithoutUnavailableMessage() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 아직 아무것도 방출하지 않는다
        val viewModel = viewModel(flow { awaitCancellation() })
        advanceUntilIdle()

        // Then 찾는 중이라 안내 문구가 뜨지 않고 읽음 처리도 하지 않는다
        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isUnavailable)
        coVerify(exactly = 0) { markNoticeRead(any()) }
    }

    @Test
    fun init_cacheHit_showsNoticeWithoutRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 캐시에 7번 공지가 있는 채로 진입한다
        val viewModel = viewModel(flowOf(listOf(item(1L), item(7L))))
        advanceUntilIdle()

        // Then 그 공지가 상태에 담기고 서버는 부르지 않는다
        assertEquals(notice(7L), viewModel.state.value.notice)
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isUnavailable)
        coVerify(exactly = 0) { refreshNotices() }
    }

    @Test
    fun init_cacheHit_marksNoticeRead() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 공지를 찾았다
        viewModel(flowOf(listOf(item(7L))))
        advanceUntilIdle()

        // Then 그 공지를 읽음 처리한다
        coVerify(exactly = 1) { markNoticeRead(7L) }
    }

    @Test
    fun init_cacheEmpty_refreshesOnceThenShowsNotice() = runTest(mainDispatcherRule.dispatcher) {
        // Given 프로세스 복원 직후처럼 캐시가 비어 있고, 재조회하면 7번 공지가 채워진다
        val cache = MutableStateFlow<List<NoticeItemVO>?>(null)
        coEvery { refreshNotices() } coAnswers {
            cache.value = listOf(item(7L))
            Result.success(Unit)
        }
        every { getNoticeItems() } returns cache
        coEvery { markNoticeRead(any()) } returns Unit

        // When 진입한다
        val viewModel = NoticeDetailViewModel(key, getNoticeItems, refreshNotices, markNoticeRead)
        advanceUntilIdle()

        // Then 한 번만 재조회하고 채워진 공지를 보여 주며 읽음 처리한다
        coVerify(exactly = 1) { refreshNotices() }
        assertEquals(notice(7L), viewModel.state.value.notice)
        coVerify(exactly = 1) { markNoticeRead(7L) }
    }

    @Test
    fun init_cacheEmpty_refreshFails_marksUnavailableWithoutRead() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비어 있고 재조회도 실패한다
        val viewModel = viewModel(
            items = flowOf(null),
            refresh = Result.failure(IOException("offline")),
        )
        advanceUntilIdle()

        // Then 공지는 없고 열 수 없음 상태가 되며 읽음 처리는 하지 않는다
        assertNull(viewModel.state.value.notice)
        assertTrue(viewModel.state.value.isUnavailable)
        coVerify(exactly = 0) { markNoticeRead(any()) }
    }

    @Test
    fun init_idNotInLoadedList_marksUnavailableWithoutReadOrRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록은 받았는데 7번이 없다(서버에서 내려간 공지)
        val viewModel = viewModel(flowOf(listOf(item(1L), item(2L))))
        advanceUntilIdle()

        // Then 열 수 없음 상태이고, 없는 id 를 읽음으로 저장하지도 서버를 다시 부르지도 않는다
        assertNull(viewModel.state.value.notice)
        assertTrue(viewModel.state.value.isUnavailable)
        coVerify(exactly = 0) { markNoticeRead(any()) }
        coVerify(exactly = 0) { refreshNotices() }
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun cacheReemits_keepsShowingNoticeAndOnlyMarksThatNoticeRead() = runTest(mainDispatcherRule.dispatcher) {
        // Given 상세가 떠 있고
        val cache = MutableStateFlow<List<NoticeItemVO>?>(listOf(item(7L)))
        val viewModel = viewModel(cache)
        advanceUntilIdle()

        // When 목록이 다시 방출된다(다른 공지가 추가됨)
        cache.value = listOf(item(7L), item(8L))
        advanceUntilIdle()

        // Then 화면 공지는 그대로이고, 읽음 처리는 이 화면의 공지에만 걸린다
        // (재방출로 다시 불려도 같은 id 의 재저장이라 결과가 같다)
        assertEquals(notice(7L), viewModel.state.value.notice)
        coVerify(atLeast = 1) { markNoticeRead(7L) }
        coVerify(exactly = 0) { markNoticeRead(8L) }
    }

    @Test
    fun init_markReadThrows_screenStaysUsable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 읽음 저장이 IO 실패로 던진다
        every { getNoticeItems() } returns flowOf(listOf(item(7L)))
        coEvery { refreshNotices() } returns Result.success(Unit)
        coEvery { markNoticeRead(any()) } throws IOException("disk")

        // When 상세에 진입한다
        val viewModel = NoticeDetailViewModel(key, getNoticeItems, refreshNotices, markNoticeRead)
        advanceUntilIdle()

        // Then 화면 내용은 그대로이고 뒤로가기도 동작한다
        assertEquals(
            "본문 7",
            viewModel.state.value.notice
                ?.content,
        )
        viewModel.effect.test {
            viewModel.processIntent(NoticeDetailIntent.ClickBack)
            assertEquals(NoticeDetailSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(flowOf(listOf(item(7L))))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(NoticeDetailIntent.ClickBack)

            assertEquals(NoticeDetailSideEffect.NavigateBack, awaitItem())
        }
    }
}
