package com.teamyg.parfait.core.ui.mvi.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.teamyg.parfait.core.ui.mvi.ContainerHost

@Composable
fun <STATE, EFFECT> ContainerHost<STATE, EFFECT>.collectState(): State<STATE> = container
    .state
    .collectAsStateWithLifecycle()

@Composable
fun <STATE, EFFECT> ContainerHost<STATE, EFFECT>.collectSideEffect(onEffect: suspend (EFFECT) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            container.effect.collect { onEffect(it) }
        }
    }
}
