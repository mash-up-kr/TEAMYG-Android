package com.teamyg.parfait.core.ui

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private data class TestState(val count: Int = 0) : UiState

private sealed interface TestIntent : UiIntent {
    data object Increase : TestIntent
}

private sealed interface TestSideEffect : UiSideEffect {
    data class Notify(val value: Int) : TestSideEffect
}

private class TestViewModel : BaseViewModel<TestState, TestIntent, TestSideEffect>(TestState()) {
    override fun processIntent(intent: TestIntent) {
        when (intent) {
            TestIntent.Increase -> updateState { copy(count = count + 1) }
        }
    }

    fun emit(value: Int) = postSideEffect(TestSideEffect.Notify(value))

    fun run(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? = launch(key = key, onError = onError, block = block)
}

class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun postSideEffect_noCollectorAtEmission_stillDeliveredOnLaterCollection() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 아무도 수집하지 않는 시점의 ViewModel
            val viewModel = TestViewModel()

            // When 이펙트를 먼저 발행하고 나중에 수집을 시작
            viewModel.emit(1)
            runCurrent()

            // Then 유실되지 않고 전달된다
            viewModel.effect.test {
                assertEquals(TestSideEffect.Notify(1), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun effect_afterCollectionEnds_doesNotReplayConsumedItems() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이펙트 하나를 이미 소비한 ViewModel
        val viewModel = TestViewModel()
        viewModel.emit(1)
        runCurrent()
        viewModel.effect.test {
            assertEquals(TestSideEffect.Notify(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // When 다시 구독하고 새 이펙트를 발행
        viewModel.effect.test {
            viewModel.emit(2)
            runCurrent()

            // Then 소비한 1 은 다시 오지 않고 2 만 온다
            assertEquals(TestSideEffect.Notify(2), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateState_reducer_appliesToLatestState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태
        val viewModel = TestViewModel()

        // When 인텐트를 두 번 처리
        viewModel.processIntent(TestIntent.Increase)
        viewModel.processIntent(TestIntent.Increase)

        // Then 누적된다(기존 API 회귀 방지)
        assertEquals(2, viewModel.state.value.count)
    }

    @Test
    fun launch_sameKeyWhileRunning_doesNotStartSecondJob() = runTest(mainDispatcherRule.dispatcher) {
        // Given 끝나지 않는 작업이 key "load" 로 돌고 있다
        val viewModel = TestViewModel()
        var secondRan = false
        viewModel.run(key = "load") { awaitCancellation() }
        runCurrent()

        // When 같은 key 로 다시 요청
        val second = viewModel.run(key = "load") { secondRan = true }
        advanceUntilIdle()

        // Then 두 번째 job 은 생기지 않고 블록도 실행되지 않는다
        assertNull(second)
        assertEquals(false, secondRan)
    }

    @Test
    fun launch_differentKeys_bothRun() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서로 다른 key 두 개
        val viewModel = TestViewModel()
        var firstRan = false
        var secondRan = false

        // When 각각 실행
        viewModel.run(key = "a") { firstRan = true }
        viewModel.run(key = "b") { secondRan = true }
        advanceUntilIdle()

        // Then 둘 다 실행된다
        assertEquals(true, firstRan)
        assertEquals(true, secondRan)
    }

    @Test
    fun launch_afterPreviousJobCompleted_sameKeyRunsAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 같은 key 의 첫 작업이 이미 끝났다
        val viewModel = TestViewModel()
        viewModel.run(key = "load") { }
        advanceUntilIdle()

        // When 같은 key 로 다시 요청
        var secondRan = false
        val second = viewModel.run(key = "load") { secondRan = true }
        advanceUntilIdle()

        // Then 실행된다(완료된 job 이 맵에서 정리됐다)
        assertNotNull(second)
        assertEquals(true, secondRan)
    }

    @Test
    fun launch_blockThrows_emitsUnexpectedError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러를 수집하는 화면
        val viewModel = TestViewModel()

        viewModel.error.test {
            // When 블록이 예상 못 한 예외를 던진다
            viewModel.run { throw IllegalStateException("boom") }
            advanceUntilIdle()

            // Then Unexpected 로 감싸져 발행된다
            val error = assertIs<AppError.Unexpected>(awaitItem())
            assertEquals("boom", error.cause?.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun launch_onErrorGiven_handlerReceivesInsteadOfChannel() = runTest(mainDispatcherRule.dispatcher) {
        // Given onError 를 넘긴 호출
        val viewModel = TestViewModel()
        var handled: AppError? = null

        // When 블록이 던진다
        viewModel.run(onError = { handled = it }) { throw IllegalStateException("boom") }
        advanceUntilIdle()

        // Then 핸들러가 받는다
        assertIs<AppError.Unexpected>(handled)
    }

    @Test
    fun launch_cancellation_doesNotEmitError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 에러를 수집하는 화면
        val viewModel = TestViewModel()

        viewModel.error.test {
            // When 블록이 CancellationException 을 던진다
            viewModel.run { throw CancellationException("cancelled") }
            advanceUntilIdle()

            // Then 에러로 오분류하지 않는다
            expectNoEvents()
        }
    }
}
