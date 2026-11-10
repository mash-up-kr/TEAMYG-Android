package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notice.NoticeItemVO
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.usecase.notice.GetNoticeItemsUseCase
import com.teamyg.parfait.domain.usecase.notice.RefreshNoticesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoticeListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNoticeItems: GetNoticeItemsUseCase = mockk()
    private val refreshNotices: RefreshNoticesUseCase = mockk()
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private fun item(
        id: Long,
        isNew: Boolean,
    ) = NoticeItemVO(
        notice = NoticeVO(
            id = id,
            title = "공지 $id",
            publishedDate = LocalDate(2026, 11, 17),
            content = "본문 $id",
        ),
        isNew = isNew,
    )

    private fun viewModel(
        items: Flow<List<NoticeItemVO>?>,
        refresh: Result<Unit> = Result.success(Unit),
    ): NoticeListViewModel {
        every { getNoticeItems() } returns items
        coEvery { refreshNotices() } returns refresh
        return NoticeListViewModel(
            getNoticeItems = getNoticeItems,
            refreshNotices = refreshNotices,
            analyticsLogger = analyticsLogger,
        )
    }

    @Test
    fun init_cacheEmpty_refreshesOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비어 있는 채로 목록이 열린다
        viewModel(MutableStateFlow(null))
        advanceUntilIdle()

        // Then 서버 갱신을 한 번 부른다
        coVerify(exactly = 1) { refreshNotices() }
    }

    @Test
    fun init_cacheLoaded_doesNotRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 설정 화면이 이미 받아 둔 캐시가 있다
        val viewModel = viewModel(flowOf(listOf(item(1L, true), item(2L, false))))
        advanceUntilIdle()

        // Then 서버를 부르지 않고 캐시를 그대로 보여준다
        coVerify(exactly = 0) { refreshNotices() }
        assertEquals(
            listOf(1L, 2L),
            viewModel.state.value.notices
                .map { it.notice.id },
        )
    }

    @Test
    fun init_refreshFails_showsLoadError() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 캐시가 비어 있고 갱신이 실패한다
        val viewModel = viewModel(MutableStateFlow(null), refresh = Result.failure(IllegalStateException("fail")))
        advanceUntilIdle()

        // Then 불러오기 실패 안내 effect 가 나간다
        viewModel.effect.test {
            assertEquals(NoticeListSideEffect.ShowLoadError, awaitItem())
        }
    }

    @Test
    fun init_emptyList_showsNothingWithoutError() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 공지가 0건이다
        val viewModel = viewModel(flowOf(emptyList()))
        advanceUntilIdle()

        // Then 빈 목록이고 서버 호출도 에러 effect 도 없다
        assertTrue(
            viewModel.state.value.notices
                .isEmpty(),
        )
        coVerify(exactly = 0) { refreshNotices() }
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun clickNotice_navigatesWithThatNotice() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록이 떠 있다
        val target = item(7L, true)
        val viewModel = viewModel(flowOf(listOf(item(1L, false), target)))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 7번 공지를 누른다
            viewModel.processIntent(NoticeListIntent.ClickNotice(id = 7L))

            // Then 그 공지 한 건을 들고 상세로 이동한다
            assertEquals(NoticeListSideEffect.NavigateToDetail(notice = target.notice), awaitItem())
        }
    }

    @Test
    fun clickNotice_newNotice_logsItemTappedWithRowIsNewTrue() = runTest(mainDispatcherRule.dispatcher) {
        // Given NEW 칩이 붙은 공지가 떠 있다
        val viewModel = viewModel(flowOf(listOf(item(1L, true))))
        advanceUntilIdle()

        // When 그 공지를 누른다
        viewModel.processIntent(NoticeListIntent.ClickNotice(id = 1L))

        // Then 공지 id 와 행의 is_new=true 로 이벤트가 나간다
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
    fun clickNotice_notNewNotice_logsItemTappedWithRowIsNewFalse() = runTest(mainDispatcherRule.dispatcher) {
        // Given NEW 칩이 없는 공지가 떠 있다
        val viewModel = viewModel(flowOf(listOf(item(2L, false))))
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
    fun clickNotice_unknownId_navigatesNowhereAndLogsNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록에 없는 id
        val viewModel = viewModel(flowOf(listOf(item(1L, true))))
        advanceUntilIdle()

        viewModel.effect.test {
            // When 그 id 를 누른다
            viewModel.processIntent(NoticeListIntent.ClickNotice(id = 99L))

            // Then 화면이 그린 적 없는 공지라 이동도 이벤트도 없다
            expectNoEvents()
        }
        verify(exactly = 0) { analyticsLogger.logEvent(any()) }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(flowOf(listOf(item(1L, true))))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(NoticeListIntent.ClickBack)

            assertEquals(NoticeListSideEffect.NavigateBack, awaitItem())
        }
    }
}
