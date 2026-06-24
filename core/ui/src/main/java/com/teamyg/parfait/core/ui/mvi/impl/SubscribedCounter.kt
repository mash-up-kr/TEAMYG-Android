package com.teamyg.parfait.core.ui.mvi.impl

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * state/effect 구독 수를 추적하는 공유 컴포넌트
 */
internal class SubscribedCounter {
    private val _subscribed = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val subscribed: Flow<Boolean> = _subscribed

    private var count = 0
    private val lock = Any()

    init {
        _subscribed.tryEmit(false)
    }

    fun increment() = synchronized(lock) {
        count++

        if (count == 1) {
            _subscribed.tryEmit(true)
        }
    }

    fun decrement() = synchronized(lock) {
        count--

        if (count == 0) {
            _subscribed.tryEmit(false)
        }
    }
}
