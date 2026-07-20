package com.teamyg.parfait.feature.app.setting.impl.viewmodel

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
data class PrivacyPolicyState(
    val url: String = "https://pleasurehun99.notion.site/3a3f9ea7234a8062bf34fcd291b1c874?pvs=73",
) : UiState

sealed interface PrivacyPolicyIntent : UiIntent {
    data object ClickBack : PrivacyPolicyIntent
}

sealed interface PrivacyPolicySideEffect : UiSideEffect {
    data object NavigateBack : PrivacyPolicySideEffect
}

@HiltViewModel
class PrivacyPolicyViewModel
@Inject
constructor() : BaseViewModel<PrivacyPolicyState, PrivacyPolicyIntent, PrivacyPolicySideEffect>(
    initialState = PrivacyPolicyState(),
) {
    init {
        viewModelLogger.i { "PrivacyPolicyViewModel::init" }
    }

    override fun processIntent(intent: PrivacyPolicyIntent) {
        when (intent) {
            PrivacyPolicyIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleClickBack() {
        postSideEffect(PrivacyPolicySideEffect.NavigateBack)
    }
}
