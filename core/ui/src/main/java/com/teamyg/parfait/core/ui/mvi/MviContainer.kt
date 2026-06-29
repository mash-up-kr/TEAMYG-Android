package com.teamyg.parfait.core.ui.mvi

import com.teamyg.parfait.core.ui.mvi.scope.IntentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MviContainer<STATE, EFFECT> {
    val state: StateFlow<STATE>
    val effect: Flow<EFFECT>

    fun intent(block: suspend IntentScope<STATE, EFFECT>.() -> Unit)

    fun repeatOnSubscription(block: suspend CoroutineScope.() -> Unit): Job
}
