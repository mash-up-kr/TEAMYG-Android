package com.teamyg.parfait.feature.splash.impl

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.splash.SplashInitialUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val loadingStatus: LoadingStatus = LoadingStatus.Loading,
) : UiState

enum class LoadingStatus {
    Loading,
    Success,
}

sealed interface SplashIntent : UiIntent

sealed interface SplashSideEffect : UiSideEffect

@HiltViewModel
class SplashViewModel
@Inject
constructor(
    splashInitialUseCase: SplashInitialUseCase,
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(initialState = SplashState()) {
    init {
        viewModelLogger.i { "SplashViewModel::init" }

        viewModelScope.launch {
            splashInitialUseCase()
            updateState { copy(loadingStatus = LoadingStatus.Success) }
        }
    }

    override fun processIntent(intent: SplashIntent) = Unit
}
