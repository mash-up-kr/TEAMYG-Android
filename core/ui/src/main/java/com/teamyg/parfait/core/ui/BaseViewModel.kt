package com.teamyg.parfait.core.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
}
