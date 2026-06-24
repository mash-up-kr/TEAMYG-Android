package com.teamyg.parfait.core.ui.mvi

import com.teamyg.parfait.core.ui.mvi.impl.BroadcastSideEffectProvider
import com.teamyg.parfait.core.ui.mvi.impl.FanOutSideEffectProvider
import com.teamyg.parfait.core.ui.mvi.impl.StrictFanOutSideEffectProvider
import com.teamyg.parfait.core.ui.mvi.impl.SubscribedCounter
import com.teamyg.parfait.core.ui.mvi.setting.MviSettings
import com.teamyg.parfait.core.ui.mvi.setting.SideEffectMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal interface SideEffectProvider<EFFECT> {
    val effectFlow: Flow<EFFECT>

    suspend fun post(effect: EFFECT)

    fun initialise(
        scope: CoroutineScope,
        counter: SubscribedCounter,
    )

    companion object {
        fun <E> create(settings: MviSettings): SideEffectProvider<E> = when (settings.sideEffectMode) {
            SideEffectMode.FAN_OUT -> FanOutSideEffectProvider(settings)
            SideEffectMode.FAN_OUT_STRICT -> StrictFanOutSideEffectProvider(settings)
            SideEffectMode.BROADCAST -> BroadcastSideEffectProvider(settings)
        }
    }
}
