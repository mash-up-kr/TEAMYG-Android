package com.teamyg.parfait.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.usecase.ui.GetLoadingFlowUseCase
import com.teamyg.parfait.domain.usecase.ui.HideLoadingUseCase
import com.teamyg.parfait.domain.usecase.ui.ShowLoadingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

data class MainState(
    val loadingState: LoadingState = LoadingState(),
) : UiState {
    data class LoadingState(
        val isLoading: Boolean = false,
        val loadingTime: Instant = Clock.System.now(),
    )
}

sealed interface MainEffect : UiSideEffect

sealed interface MainIntent : UiIntent

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val getLoadingFlowUseCase: GetLoadingFlowUseCase,
    private val showLoadingUseCase: ShowLoadingUseCase,
    private val hideLoadingUseCase: HideLoadingUseCase,
) : BaseViewModel<MainState, MainIntent, MainEffect>(initialState = MainState()) {
    init {
        viewModelScope.launch { collectLoading() }

        viewModelScope.launch {
            val tag = "MainViewModel"
            delay(500.milliseconds)
            showLoadingUseCase(tag = tag)
            delay(5000.milliseconds)
            hideLoadingUseCase(tag = tag)
        }
    }

    override fun processIntent(intent: MainIntent) = Unit

    private suspend fun collectLoading() = getLoadingFlowUseCase().collect {
        updateState {
            copy(
                loadingState = loadingState.copy(
                    isLoading = it,
                    loadingTime = Clock.System.now(),
                ),
            )
        }
    }
}
