package com.teamyg.parfait.core.ui.mvi.impl

import com.teamyg.parfait.core.ui.mvi.SideEffectProvider
import com.teamyg.parfait.core.ui.mvi.setting.MviSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class FanOutSideEffectProvider<EFFECT>(settings: MviSettings) : SideEffectProvider<EFFECT> {
    private val channel = Channel<EFFECT>(settings.sideEffectBufferSize)
    override val effectFlow: Flow<EFFECT> = channel.receiveAsFlow()

    override suspend fun post(effect: EFFECT) = channel.send(effect)

    override fun initialise(
        scope: CoroutineScope,
        counter: SubscribedCounter,
    ) = Unit
}

internal class StrictFanOutSideEffectProvider<EFFECT>(settings: MviSettings) : SideEffectProvider<EFFECT> {
    private val channel = Channel<EFFECT>(settings.sideEffectBufferSize)
    override val effectFlow: Flow<EFFECT> = channel.consumeAsFlow()

    override suspend fun post(effect: EFFECT) = channel.send(effect)

    override fun initialise(
        scope: CoroutineScope,
        counter: SubscribedCounter,
    ) = Unit
}

internal class BroadcastSideEffectProvider<EFFECT>(
    private val settings: MviSettings,
) : SideEffectProvider<EFFECT> {
    private val sharedFlow = MutableSharedFlow<EFFECT>(
        replay = resolveBufferSize(settings.sideEffectBufferSize),
        onBufferOverflow = BufferOverflow.SUSPEND,
    )

    override val effectFlow: Flow<EFFECT> = sharedFlow

    override suspend fun post(effect: EFFECT) = sharedFlow.emit(effect)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initialise(
        scope: CoroutineScope,
        counter: SubscribedCounter,
    ) {
        scope.launch {
            var previous = false
            counter.subscribed.collect { subscribed ->
                if (!previous && subscribed) {
                    if (settings.replayClearDelayMillis > 0) {
                        delay(settings.replayClearDelayMillis.milliseconds)
                    }
                    sharedFlow.resetReplayCache()
                }
                previous = subscribed
            }
        }
    }
}

private const val DEFAULT_BUFFER_SIZE = 64

private fun resolveBufferSize(size: Int): Int = if (size < 0) DEFAULT_BUFFER_SIZE else size
