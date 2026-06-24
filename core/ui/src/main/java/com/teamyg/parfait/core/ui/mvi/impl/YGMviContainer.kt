package com.teamyg.parfait.core.ui.mvi.impl

import com.teamyg.parfait.core.ui.mvi.MviContainer
import com.teamyg.parfait.core.ui.mvi.SideEffectProvider
import com.teamyg.parfait.core.ui.mvi.setting.MviSettings
import com.teamyg.parfait.core.ui.mvi.scope.IntentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAtomicApi::class)
internal class YGMviContainer<STATE, EFFECT>(
    initialState: STATE,
    private val scope: CoroutineScope,
    private val settings: MviSettings,
    private val onCreate: (suspend IntentScope<STATE, EFFECT>.() -> Unit)?,
) : MviContainer<STATE, EFFECT> {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state.asStateFlow()

    private val subscribedCounter: SubscribedCounter = SubscribedCounter()
    private val sideEffectProvider: SideEffectProvider<EFFECT> = SideEffectProvider.create(settings)

    override val effect: Flow<EFFECT> = countingFlow(sideEffectProvider.effectFlow, subscribedCounter)

    private val intentChannel: Channel<suspend IntentScope<STATE, EFFECT>.() -> Unit> =
        Channel<suspend IntentScope<STATE, EFFECT>.() -> Unit>(
            capacity = Channel.UNLIMITED,
        )

    private val initialized: AtomicBoolean = AtomicBoolean(false)

    private val intentScope = object : IntentScope<STATE, EFFECT> {
        override val state: STATE get() = _state.value

        override fun reduce(reducer: STATE.() -> STATE) = _state.update { it.reducer() }

        override suspend fun postSideEffect(effect: EFFECT) = sideEffectProvider.post(effect)
    }

    init {
        sideEffectProvider.initialise(
            scope = scope,
            counter = subscribedCounter,
        )

        scope.launch {
            for (block in intentChannel) {
                runCatching { intentScope.block() }
                    .onFailure {
                        // TODO 필요시 에러 로깅
                        throw it
                    }
            }
        }
    }

    override fun intent(block: suspend IntentScope<STATE, EFFECT>.() -> Unit): Job {
        initializeIfNeeded()
        intentChannel.trySend(block)
        return Job()
    }

    private fun initializeIfNeeded() {
        if (onCreate != null && initialized.compareAndSet(expectedValue = false, newValue = true)) {
            intentChannel.trySend(onCreate)
        }
    }

    private fun combinedSubscription(): Flow<Boolean> = combine(
        _state.subscriptionCount,
        subscribedCounter.subscribed,
    ) { stateCount, effectSubscribed ->
        stateCount > 0 || effectSubscribed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun repeatOnSubscription(block: suspend CoroutineScope.() -> Unit): Job = scope.launch {
        var previous = false

        combinedSubscription()
            .mapLatest { subscribed ->
                if (!subscribed && previous) {
                    delay(settings.subscriptionStopTimeoutMillis.milliseconds)
                }
                previous = subscribed
                subscribed
            }.collect { subscribed ->
                if (subscribed) {
                    block()
                }
            }
    }

    private fun <T> countingFlow(
        upstream: Flow<T>,
        counter: SubscribedCounter,
    ): Flow<T> = object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            counter.increment()
            try {
                upstream.collect(collector)
            } finally {
                counter.decrement()
            }
        }
    }
}
