package com.tjyg.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


abstract class BaseViewModel<S: UiState, I: UiIntent, E: UiSideEffect>(
    initialState: S
) : ViewModel(){

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _effect = Channel<E>()
    val effect = _effect.receiveAsFlow()

    abstract fun processIntent(intent: I)

    protected fun updateState(reducer: S.() -> S){
        _state.value = _state.value.reducer()
    }

    protected fun postSideEffect(effect: E){
        viewModelScope.launch { _effect.send(effect) }
    }
}