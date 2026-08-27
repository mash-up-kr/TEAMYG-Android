package com.teamyg.parfait.core.ui

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

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

private data class ProbeState(val value: Int = 0) : UiState

private object ProbeIntent : UiIntent

private object ProbeEffect : UiSideEffect

@OptIn(ExperimentalCoroutinesApi::class)
private class ProbeViewModel(
    private val upstream: Flow<Int>,
    private val readsOwnState: Boolean = false,
) : BaseViewModel<ProbeState, ProbeIntent, ProbeEffect>(ProbeState()) {
    var openCount = 0
        private set

    init {
        launchWhileSubscribed(
            source = {
                openCount++
                if (readsOwnState) state.map { it.value }.flatMapLatest { upstream } else upstream
            },
            collector = { value -> updateState { copy(value = value) } },
        )
    }

    override fun processIntent(intent: ProbeIntent) = Unit
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
    fun launch_blockThrows_onErrorReceivesUnexpected() = runTest(mainDispatcherRule.dispatcher) {
        // Given 실패를 받아 갈 onError 를 넘긴 화면
        val viewModel = TestViewModel()
        var handled: AppError? = null

        // When 블록이 예상 못 한 예외를 던진다
        viewModel.run(onError = { handled = it }) { throw IllegalStateException("boom") }
        advanceUntilIdle()

        // Then Unexpected 로 감싸져 전달된다
        val error = assertIs<AppError.Unexpected>(handled)
        assertEquals("boom", error.cause?.message)
    }

    @Test
    fun launch_childCoroutineThrows_onErrorReceivesUnexpected() = runTest(mainDispatcherRule.dispatcher) {
        // Given 실패를 받아 갈 onError 를 넘긴 화면
        val viewModel = TestViewModel()
        var handled: AppError? = null

        // When block 이 자식 코루틴을 띄우고 그 자식이 예상 못 한 예외를 던진다
        viewModel.run(onError = { handled = it }) { launch { throw IllegalStateException("boom") } }
        advanceUntilIdle()

        // Then 부모로 전파돼 Unexpected 로 전달된다(coroutineScope 가 원래 예외를 재던진다)
        val error = assertIs<AppError.Unexpected>(handled)
        assertEquals("boom", error.cause?.message)
    }

    @Test
    fun launch_onErrorOmitted_doesNotEmitSideEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이펙트를 수집하는 화면
        val viewModel = TestViewModel()

        viewModel.effect.test {
            // When onError 없이 부른 블록이 던진다
            viewModel.run { throw IllegalStateException("boom") }
            advanceUntilIdle()

            // Then 로그만 남고 이펙트 스트림은 오염되지 않는다 — 실패를 어떤 동작으로
            // 옮길지는 화면이 onError 로 정한다
            expectNoEvents()
        }
    }

    @Test
    fun launch_cancellation_doesNotReachOnError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 실패를 받아 갈 onError 를 넘긴 화면
        val viewModel = TestViewModel()
        var handled: AppError? = null

        // When 블록이 CancellationException 을 던진다
        viewModel.run(onError = { handled = it }) { throw CancellationException("cancelled") }
        advanceUntilIdle()

        // Then 취소를 에러로 오분류하지 않는다
        assertNull(handled)
    }

    @Test
    fun launchWhileSubscribed_withoutSubscribers_doesNotOpenTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProbeViewModel(MutableStateFlow(1))
        advanceUntilIdle()

        assertEquals(0, viewModel.openCount)
    }

    @Test
    fun launchWhileSubscribed_withASubscriber_opensTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val job = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        assertEquals(1, viewModel.openCount)
        assertEquals(1, viewModel.state.value.value)
        job.cancel()
    }

    @Test
    fun launchWhileSubscribed_afterTheTimeout_closesTheUpstream() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val job = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        job.cancel()
        advanceTimeBy(10.seconds)
        advanceUntilIdle()

        upstream.value = 2
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.value)
    }

    @Test
    fun launchWhileSubscribed_resubscribedWithinTheTimeout_doesNotReopen() = runTest(mainDispatcherRule.dispatcher) {
        val upstream = MutableStateFlow(1)
        val viewModel = ProbeViewModel(upstream)

        val first = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        first.cancel()
        advanceTimeBy(1.seconds)

        val second = backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        assertEquals(1, viewModel.openCount)
        second.cancel()
    }

    // launchWhileSubscribed_whenTheSourceReadsOwnState_stillCloses 는 여기 두지 않는다.
    // source 가 state 를 구독하면 그 구독 자체가 subscriptionCount 를 다시 올려 유예 타이머가
    // 영영 만료되지 않는다 — 헬퍼 구현만으로는 못 막는 자기 고착이다. 계약은 코드가 아니라
    // BaseViewModel.launchWhileSubscribed 의 KDoc 경고("source 안에서 state 를 수집하면 안
    // 된다")로 대신 세운다.
}
