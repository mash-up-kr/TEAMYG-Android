package com.teamyg.parfait.core.ui.mvi.scope

import com.teamyg.parfait.core.ui.mvi.annotation.MviDsl

@MviDsl
interface IntentScope<STATE, EFFECT> {
    val state: STATE

    fun reduce(reducer: STATE.() -> STATE)

    suspend fun postSideEffect(effect: EFFECT)
}
