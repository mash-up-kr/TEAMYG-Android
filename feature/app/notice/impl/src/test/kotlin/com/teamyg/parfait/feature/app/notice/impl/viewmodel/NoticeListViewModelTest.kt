package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.usecase.notice.GetNoticesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoticeListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNotices: GetNoticesUseCase = mockk()
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private val notice = NoticeVO(
        id = 1L,
        title = "공지",
        date = "2026.11.17",
        content = "본문",
        isNew = true,
    )

    private fun viewModel(result: Result<List<NoticeVO>>): NoticeListViewModel {
        coEvery { getNotices() } returns result
        return NoticeListViewModel(getNotices = getNotices, analyticsLogger = analyticsLogger)
    }

    @Test
    fun init_loadsNoticesIntoState() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 공지 두 건이 있는 상태로 진입한다
        val viewModel = viewModel(Result.success(listOf(notice, notice.copy(id = 2L, isNew = false))))
        advanceUntilIdle()

        // Then 목록이 상태에 담긴다
        assertEquals(
            listOf(1L, 2L),
            viewModel.state.value.notices
                .map { it.id },
        )
    }

    @Test
    fun init_emptyList_keepsEmptyStateWithoutError() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 공지가 0건이다
        val viewModel = viewModel(Result.success(emptyList()))
        advanceUntilIdle()

        // Then 빈 목록이고 에러 effect 는 나가지 않는다
        assertTrue(
            viewModel.state.value.notices
                .isEmpty(),
        )
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun init_failure_postsShowLoadError() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 조회가 실패한다
        val viewModel = viewModel(Result.failure(IllegalStateException("fail")))
        advanceUntilIdle()

        // Then 불러오기 실패 안내 effect 가 나간다
        viewModel.effect.test {
            assertEquals(NoticeListSideEffect.ShowLoadError, awaitItem())
        }
    }

    @Test
    fun clickNotice_postsNavigateToDetailWithThatId() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록이 떠 있다
        val viewModel = viewModel(Result.success(listOf(notice)))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 공지를 누른다
            viewModel.processIntent(NoticeListIntent.ClickNotice(id = 7L))

            // Then 그 id 의 상세로 이동한다
            assertEquals(NoticeListSideEffect.NavigateToDetail(id = 7L), awaitItem())
        }
    }

    @Test
    fun clickNotice_newNotice_logsItemTappedWithIsNewTrue() = runTest(mainDispatcherRule.dispatcher) {
        // Given NEW 칩이 붙은 공지가 떠 있다
        val viewModel = viewModel(Result.success(listOf(notice)))
        advanceUntilIdle()

        // When 그 공지를 누른다
        viewModel.processIntent(NoticeListIntent.ClickNotice(id = 1L))

        // Then 공지 id 와 is_new=true 로 이벤트가 나간다
        verify(exactly = 1) {
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.APP_NOTICE_ITEM_TAPPED,
                    params = mapOf("notice_id" to "1", "is_new" to "true"),
                ),
            )
        }
    }

    @Test
    fun clickNotice_readNotice_logsItemTappedWithIsNewFalse() = runTest(mainDispatcherRule.dispatcher) {
        // Given NEW 칩이 없는 공지가 떠 있다
        val viewModel = viewModel(Result.success(listOf(notice.copy(id = 2L, isNew = false))))
        advanceUntilIdle()

        // When 그 공지를 누른다
        viewModel.processIntent(NoticeListIntent.ClickNotice(id = 2L))

        // Then is_new=false 로 이벤트가 나간다
        verify(exactly = 1) {
            analyticsLogger.logEvent(
                AnalyticsEvent(
                    type = AnalyticsEventType.APP_NOTICE_ITEM_TAPPED,
                    params = mapOf("notice_id" to "2", "is_new" to "false"),
                ),
            )
        }
    }

    @Test
    fun clickNotice_unknownId_navigatesWithoutLogging() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록에 없는 id
        val viewModel = viewModel(Result.success(listOf(notice)))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 그 id 를 누른다
            viewModel.processIntent(NoticeListIntent.ClickNotice(id = 99L))

            // Then 이동은 하고 이벤트는 나가지 않는다
            assertEquals(NoticeListSideEffect.NavigateToDetail(id = 99L), awaitItem())
        }
        verify(exactly = 0) { analyticsLogger.logEvent(any()) }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(Result.success(listOf(notice)))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(NoticeListIntent.ClickBack)

            assertEquals(NoticeListSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickBack_afterLoadFailure_stillNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회 실패로 안내 effect 가 이미 나간 상태
        val viewModel = viewModel(Result.failure(IllegalStateException("fail")))
        advanceUntilIdle()

        viewModel.effect.test {
            assertEquals(NoticeListSideEffect.ShowLoadError, awaitItem())

            // When 뒤로가기를 누른다
            viewModel.processIntent(NoticeListIntent.ClickBack)

            // Then 정상적으로 뒤로 간다
            assertEquals(NoticeListSideEffect.NavigateBack, awaitItem())
        }
    }
}
