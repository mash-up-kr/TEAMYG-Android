package com.teamyg.parfait.feature.splash.impl

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashState(
    val loadingStatus: LoadingStatus = LoadingStatus.Loading,
) : UiState

enum class LoadingStatus {
    Loading, Success, Failure
}

sealed interface SplashIntent : UiIntent

sealed interface SplashSideEffect : UiSideEffect

@HiltViewModel
class SplashViewModel
@Inject
constructor(
) : BaseViewModel<SplashState, SplashIntent, SplashSideEffect>(initialState = SplashState()) {

    init {
        viewModelScope.launch {
            // 스플래시 관련 작업
        }
    }

    override fun processIntent(intent: SplashIntent) = Unit
}
