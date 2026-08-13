package com.teamyg.parfait.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

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

    private val _error = Channel<AppError>(Channel.BUFFERED)

    /**
     * 화면이 따로 선언하지 않아도 되는 공통 실패 통로. `E` 와 분리해 두면 화면마다
     * `SideEffect` 에 `ShowError` 를 중복 선언하지 않아도 된다. 수집은 `CollectAppError`.
     */
    val error: Flow<AppError> = _error.receiveAsFlow()

    /** `viewModelScope` 는 `Main.immediate` 라 이 맵 접근은 항상 메인 스레드 단일이다 */
    private val runningJobs = mutableMapOf<Any, Job>()

    protected fun postError(error: AppError) {
        if (_error.trySend(error).isFailure) {
            viewModelLogger.e { "에러 버퍼가 가득 차 드롭됐다: $error" }
        }
    }

    /**
     * ViewModel 작업을 실행한다. UI 이벤트를 코루틴으로 옮기는 상태홀더 경계라
     * `viewModelScope` 를 쓰는 것이 맞다.
     *
     * @param key 같은 key 의 작업이 아직 돌고 있으면 **새로 시작하지 않고 `null` 을 반환**한다
     *   (버튼 연타로 인한 중복 호출 차단). `null` 이면 중복 검사를 하지 않는다.
     * @param onError 예상 못 한 예외 처리. 없으면 [postError] 로 흘린다.
     *
     * `Result.failure` 는 값이지 예외가 아니므로 여기서 잡히지 않는다 — 호출부가 명시적으로
     * 처리한다. 이 가드는 매퍼 버그·NPE 같은 *예상 못 한* 예외용이다.
     */
    protected fun launch(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job? {
        if (key != null && runningJobs[key]?.isActive == true) return null

        val job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val appError = e as? AppError ?: AppError.Unexpected(e)
                if (onError != null) onError(appError) else postError(appError)
            }
        }

        if (key != null) {
            runningJobs[key] = job
            // 같은 key 로 이미 다음 job 이 등록됐다면 그것을 지우면 안 된다
            job.invokeOnCompletion { if (runningJobs[key] === job) runningJobs.remove(key) }
        }
        return job
    }
}
