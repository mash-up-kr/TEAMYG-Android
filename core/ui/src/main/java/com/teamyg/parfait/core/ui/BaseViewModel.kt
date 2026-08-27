package com.teamyg.parfait.core.ui

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    private val effectSubscribers = AtomicInteger(0)

    /**
     * 1회성 효과. **화면당 한 곳(Route)에서만 수집한다.**
     *
     * `Channel` 인 이유: 구독자가 없는 순간 발행해도 버퍼에 남았다가 전달되고, 이미 소비한
     * 이펙트는 재구독해도 다시 오지 않는다. `SharedFlow` + `replay` 는 후자를 깨서
     * 화면 재진입·Activity 재생성 때 내비게이션이 저절로 다시 실행된다.
     *
     * 대신 단일 소비자다 — 두 곳에서 수집하면 이펙트가 한쪽에만 간다. 조용히 넘어가지
     * 않도록 동시 구독자 수를 세어 로그를 남긴다.
     */
    val effect: Flow<E> = _effect
        .receiveAsFlow()
        .onStart {
            val count = effectSubscribers.incrementAndGet()
            if (count > 1) {
                viewModelLogger.e { "effect 를 ${count}곳에서 수집한다 — 이펙트가 한쪽에만 전달된다" }
            }
        }.onCompletion { effectSubscribers.decrementAndGet() }

    abstract fun processIntent(intent: I)

    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun postSideEffect(effect: E) {
        if (_effect.trySend(effect).isFailure) {
            viewModelLogger.e { "이펙트 버퍼가 가득 차 드롭됐다: $effect" }
        }
    }

    /**
     * [launch] 는 항상 메인 스레드에서 호출된다는 것이 이 맵의 스레드 안전 불변식이다
     * (`viewModelScope` 가 `Main.immediate` 라서가 아니다 — 그건 `block` 이 도는
     * 디스패처일 뿐, `runningJobs[key] = job` 줄 자체는 코루틴 밖, 즉 `launch` 를
     * 호출한 스레드에서 동기적으로 돈다).
     */
    private val runningJobs = mutableMapOf<Any, Job>()

    /**
     * ViewModel 작업을 실행한다. UI 이벤트를 코루틴으로 옮기는 상태홀더 경계라
     * `viewModelScope` 를 쓰는 것이 맞다.
     *
     * @param key 같은 key 의 작업이 아직 돌고 있으면 **새로 시작하지 않고 `null` 을 반환**한다
     *   (버튼 연타로 인한 중복 호출 차단). `null` 이면 중복 검사를 하지 않는다.
     * @param onError 예상 못 한 예외 처리. **화면이 실패를 표현해야 하면 여기서 자기
     *   `SideEffect` 를 발행한다** — 예: `launch(onError = { postSideEffect(Xxx.ShowError(it)) })`.
     *   넘기지 않으면 로그만 남는다.
     *
     * 베이스가 공용 에러 스트림을 따로 두지 않는 이유: 실패도 1회성 효과라 이펙트와 성질이
     * 같은데, 스트림을 나누면 둘 사이 순서 보장이 사라지고 실패 경로가 아예 없는 화면까지
     * 빈 채널을 하나씩 달게 된다. 실패를 어떤 동작으로 옮길지는 화면의 어휘(`E`)가 정한다.
     *
     * `Result.failure` 는 값이지 예외가 아니므로 여기서 잡히지 않는다 — 호출부가 명시적으로
     * 처리한다. 이 가드는 매퍼 버그·NPE 같은 *예상 못 한* 예외용이다.
     */
    @MainThread
    protected fun launch(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? {
        if (key != null && runningJobs[key]?.isActive == true) return null

        val job = viewModelScope.launch {
            try {
                // `coroutineScope` 로 감싸야 `block` 안에서 띄운 자식 코루틴의 실패가
                // (부모 job 취소 후 여기가 CancellationException 으로 재개되는 대신)
                // 이 호출 지점으로 원래 예외 그대로 재던져진다.
                coroutineScope { block() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val appError = e as? AppError ?: AppError.Unexpected(e)
                if (onError != null) {
                    onError(appError)
                } else {
                    // TODO(에러 UX 미정): 화면이 표현을 정하면 그 화면이 onError 를 넘긴다
                    viewModelLogger.e(e) { "처리되지 않은 실패: $appError" }
                }
            }
        }

        if (key != null) {
            // production 의 `Main.immediate` 에서는 여기 있는 non-suspending 블록이
            // 메인 스레드에서 동기적으로 끝까지 실행된다 — `runningJobs[key] = job` 이
            // 실행되기 전에 `job` 이 이미 완료될 수는 없다. 그래서 `invokeOnCompletion`
            // 을 `put` 뒤에 등록해도 순서가 깨지지 않는다. (단, `runTest` 처럼
            // `StandardTestDispatcher` 를 쓰는 테스트에서는 `job` 이 즉시 시작되지
            // 않으므로 이 순서 보장에 기대는 테스트를 짜지 않는다.)
            runningJobs[key] = job
            // 같은 key 로 이미 다음 job 이 등록됐다면 그것을 지우면 안 된다
            job.invokeOnCompletion { if (runningJobs[key] === job) runningJobs.remove(key) }
        }
        return job
    }

    /**
     * 화면이 **실제로 보고 있는 동안에만** [source] 를 연다. 라우트가
     * `collectAsStateWithLifecycle()` 로 [state] 를 구독하므로, 화면이 백그라운드로 가거나
     * 컴포지션에서 빠지면 여기서 연 업스트림도 함께 끊긴다.
     *
     * [launch] 와 갈라 두는 이유는 수명이 다르기 때문이다 — [launch] 는 ViewModel 수명이라
     * 백스택 아래에 깔린 화면에서도 계속 돈다(`architecture/state-management.md`).
     *
     * ⚠️ **[source] 안에서 [state] 를 수집하면 안 된다.** 활성 조건이 [state] 의 구독자 수라,
     * 열린 업스트림 자신이 구독자로 세어져 계수가 0 으로 내려가지 않는다. 화면 조건으로 업스트림을
     * 가르려면 [state] 가 아닌 별도 flow 를 둔다.
     *
     * @param stopTimeout 마지막 구독자가 떠난 뒤 업스트림을 닫기까지의 유예. 화면 전환·구성
     *   변경의 짧은 공백에서 업스트림이 껐다 켜지지 않게 한다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> launchWhileSubscribed(
        stopTimeout: Duration = SUBSCRIPTION_STOP_TIMEOUT,
        source: () -> Flow<T>,
        collector: suspend (T) -> Unit,
    ): Job = viewModelScope.launch {
        _state.subscriptionCount
            .map { it > 0 }
            .distinctUntilChanged()
            .flatMapLatest { subscribed ->
                // 구독이 끊겨도 유예 동안은 열어 둔다. 그 사이 다시 붙으면 아래 flatMapLatest 가
                // 이 대기를 취소하므로 업스트림이 이어진다
                if (subscribed) {
                    flowOf(true)
                } else {
                    flow {
                        delay(stopTimeout)
                        emit(false)
                    }
                }
            }.distinctUntilChanged()
            .flatMapLatest { active -> if (active) source() else emptyFlow() }
            .collect(collector)
    }

    private companion object {
        val SUBSCRIPTION_STOP_TIMEOUT: Duration = 5.seconds
    }
}
