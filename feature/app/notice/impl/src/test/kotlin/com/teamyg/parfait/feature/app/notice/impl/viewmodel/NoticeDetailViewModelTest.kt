package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.notice.NoticeVO
import com.teamyg.parfait.domain.usecase.notice.GetNoticeUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoticeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getNotice: GetNoticeUseCase = mockk()

    private val notice = NoticeVO(
        id = 7L,
        title = "공지",
        date = "2026.11.12",
        content = "본문",
        isNew = false,
    )

    private fun viewModel(result: Result<NoticeVO>): NoticeDetailViewModel {
        coEvery { getNotice(any()) } returns result
        return NoticeDetailViewModel(noticeId = 7L, getNotice = getNotice)
    }

    @Test
    fun init_loadsNoticeForGivenId() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When id 7 로 진입한다
        val viewModel = viewModel(Result.success(notice))
        advanceUntilIdle()

        // Then 그 id 로 조회하고 결과가 상태에 담긴다
        coVerify(exactly = 1) { getNotice(7L) }
        assertEquals(notice, viewModel.state.value.notice)
    }

    @Test
    fun init_failure_postsShowLoadErrorAndKeepsNullNotice() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 없는 id 라 조회가 실패한다
        val viewModel = viewModel(Result.failure(NoSuchElementException("none")))
        advanceUntilIdle()

        // Then 공지는 비어 있고 안내 effect 만 나간다
        assertNull(viewModel.state.value.notice)
        viewModel.effect.test {
            assertEquals(NoticeDetailSideEffect.ShowLoadError, awaitItem())
        }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(Result.success(notice))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(NoticeDetailIntent.ClickBack)

            assertEquals(NoticeDetailSideEffect.NavigateBack, awaitItem())
        }
    }
}
