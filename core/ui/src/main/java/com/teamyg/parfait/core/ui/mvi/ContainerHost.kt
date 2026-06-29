package com.teamyg.parfait.core.ui.mvi

import com.teamyg.parfait.core.ui.mvi.scope.IntentScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface ContainerHost<STATE, EFFECT> {
    val container: MviContainer<STATE, EFFECT>

    fun intent(block: suspend IntentScope<STATE, EFFECT>.() -> Unit) = container.intent(block)

    fun repeatOnSubscription(block: suspend CoroutineScope.() -> Unit): Job = container.repeatOnSubscription(block)
}
