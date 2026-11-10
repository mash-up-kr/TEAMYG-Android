package com.teamyg.parfait.feature.app.notice.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.notice.MarkNoticeReadUseCase
import com.teamyg.parfait.feature.app.notice.api.NavKeyNoticeDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class NoticeDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val markNoticeRead: MarkNoticeReadUseCase = mockk()

    private val key = NavKeyNoticeDetail(
        noticeId = 7L,
        title = "공지",
        date = "2026.11.12",
        content = "본문",
    )

    private fun viewModel(): NoticeDetailViewModel {
        coEvery { markNoticeRead(any()) } returns Unit
        return NoticeDetailViewModel(key = key, markNoticeRead = markNoticeRead)
    }

    @Test
    fun init_buildsStateFromKey() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 공지 한 건을 NavKey 로 받아 진입한다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 서버나 UseCase 조회 없이 NavKey 값으로 상태가 만들어진다
        assertEquals(NoticeDetailState(title = "공지", date = "2026.11.12", content = "본문"), viewModel.state.value)
    }

    @Test
    fun init_marksNoticeRead() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 상세에 진입한다
        viewModel()
        advanceUntilIdle()

        // Then 그 공지를 읽음 처리한다
        coVerify(exactly = 1) { markNoticeRead(7L) }
    }

    @Test
    fun init_markReadThrows_screenStaysUsable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 읽음 저장이 IO 실패로 던진다
        coEvery { markNoticeRead(any()) } throws IOException("disk")

        // When 상세에 진입한다
        val viewModel = NoticeDetailViewModel(key = key, markNoticeRead = markNoticeRead)
        advanceUntilIdle()

        // Then 화면 내용은 그대로이고 뒤로가기도 동작한다
        assertEquals("본문", viewModel.state.value.content)
        viewModel.effect.test {
            viewModel.processIntent(NoticeDetailIntent.ClickBack)
            assertEquals(NoticeDetailSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickBack_postsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(NoticeDetailIntent.ClickBack)

            assertEquals(NoticeDetailSideEffect.NavigateBack, awaitItem())
        }
    }
}
