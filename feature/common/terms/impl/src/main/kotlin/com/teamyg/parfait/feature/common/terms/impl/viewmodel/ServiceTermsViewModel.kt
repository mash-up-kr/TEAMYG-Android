package com.teamyg.parfait.feature.common.terms.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property url TODO 실제 Notion 공개 URL 미확정 — placeholder. 추후 UseCase 주입으로 교체.
 */
data class ServiceTermsState(
    val url: String = "https://pleasurehun99.notion.site/3a3f9ea7234a806582aae9db51717276",
) : UiState

sealed interface ServiceTermsIntent : UiIntent {
    data object ClickBack : ServiceTermsIntent
}

sealed interface ServiceTermsSideEffect : UiSideEffect {
    data object NavigateBack : ServiceTermsSideEffect
}

@HiltViewModel
class ServiceTermsViewModel
@Inject
constructor() : BaseViewModel<ServiceTermsState, ServiceTermsIntent, ServiceTermsSideEffect>(
    initialState = ServiceTermsState(),
) {
    init {
        viewModelLogger.i { "ServiceTermsViewModel::init" }
    }

    override fun processIntent(intent: ServiceTermsIntent) {
        when (intent) {
            ServiceTermsIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleClickBack() {
        postSideEffect(ServiceTermsSideEffect.NavigateBack)
    }
}
