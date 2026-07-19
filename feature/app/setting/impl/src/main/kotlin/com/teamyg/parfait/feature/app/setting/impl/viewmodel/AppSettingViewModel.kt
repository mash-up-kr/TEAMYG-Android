package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property nickname TODO 프로필 API 연동 전 placeholder 데이터
 * @property loginProvider
 * @property version TODO BuildConfig.VERSION_NAME 주입으로 교체
 */
data class AppSettingState(
    val nickname: String = "아니야나그런데기니야",
    val loginProvider: String = "Kakao",
    val version: String = "1.0v",
) : UiState

sealed interface AppSettingIntent : UiIntent {
    data object ClickBack : AppSettingIntent

    data object ClickAccount : AppSettingIntent

    data object ClickServiceTerms : AppSettingIntent

    data object ClickPrivacyPolicy : AppSettingIntent
}

sealed interface AppSettingSideEffect : UiSideEffect {
    data object NavigateBack : AppSettingSideEffect

    data object NavigateToAccountInfo : AppSettingSideEffect

    data object NavigateToServiceTerms : AppSettingSideEffect

    data object NavigateToPrivacyPolicy : AppSettingSideEffect
}

@HiltViewModel
class AppSettingViewModel
@Inject
constructor() : BaseViewModel<AppSettingState, AppSettingIntent, AppSettingSideEffect>(
    initialState = AppSettingState(),
) {
    init {
        viewModelLogger.i { "AppSettingViewModel::init" }
    }

    override fun processIntent(intent: AppSettingIntent) {
        when (intent) {
            AppSettingIntent.ClickBack -> handleClickBack()
            AppSettingIntent.ClickAccount -> handleClickAccount()
            AppSettingIntent.ClickServiceTerms -> handleClickServiceTerms()
            AppSettingIntent.ClickPrivacyPolicy -> handleClickPrivacyPolicy()
        }
    }

    private fun handleClickBack() {
        postSideEffect(AppSettingSideEffect.NavigateBack)
    }

    private fun handleClickAccount() {
        postSideEffect(AppSettingSideEffect.NavigateToAccountInfo)
    }

    private fun handleClickServiceTerms() {
        postSideEffect(AppSettingSideEffect.NavigateToServiceTerms)
    }

    private fun handleClickPrivacyPolicy() {
        postSideEffect(AppSettingSideEffect.NavigateToPrivacyPolicy)
    }
}
