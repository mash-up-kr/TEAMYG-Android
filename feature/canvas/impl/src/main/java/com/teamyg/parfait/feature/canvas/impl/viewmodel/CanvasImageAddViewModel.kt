package com.teamyg.parfait.feature.canvas.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.vmLogger
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

class CanvasImageAddState : UiState

sealed interface CanvasImageAddEffect : UiSideEffect {
    class NavigateToCamera : CanvasImageAddEffect

    class NavigateToCanvas : CanvasImageAddEffect

    data class NavigateToSegmentation(
        val uri: String,
    ) : CanvasImageAddEffect
}

sealed interface CanvasImageAddIntent : UiIntent {
    data class CacheImage(
        val uri: String,
    ) : CanvasImageAddIntent

    class OnClickCamera : CanvasImageAddIntent

    class OnClickCanvas : CanvasImageAddIntent
}

@HiltViewModel
class CanvasImageAddViewModel
@Inject
constructor(
    private val addRecentImageUseCase: AddRecentImageUseCase,
) : BaseViewModel<CanvasImageAddState, CanvasImageAddIntent, CanvasImageAddEffect>(
    initialState = CanvasImageAddState(),
) {
    init {
        vmLogger.i { "CanvasImageAddViewModel::init" }
    }

    override fun processIntent(intent: CanvasImageAddIntent) {
        when (intent) {
            is CanvasImageAddIntent.CacheImage -> handleCacheImage(intent)
            is CanvasImageAddIntent.OnClickCamera -> handleOnClickCamera()
            is CanvasImageAddIntent.OnClickCanvas -> handleOnClickCanvas()
        }
    }

    private fun handleCacheImage(intent: CanvasImageAddIntent.CacheImage) {
        viewModelScope.launch {
            addRecentImageUseCase(intent.uri)
            postSideEffect(
                effect = CanvasImageAddEffect.NavigateToSegmentation(intent.uri),
            )
        }
    }

    private fun handleOnClickCamera() {
        postSideEffect(
            effect = CanvasImageAddEffect.NavigateToCamera(),
        )
    }

    private fun handleOnClickCanvas() {
        postSideEffect(
            effect = CanvasImageAddEffect.NavigateToCanvas(),
        )
    }
}
