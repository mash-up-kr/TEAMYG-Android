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
 * @property isWithdrawDialogVisible 서비스 탈퇴 확인 팝업 노출 여부
 */
data class AppSettingState(
    val nickname: String = "아니야나그런데기니야",
    val loginProvider: String = "Kakao",
    val version: String = "1.0v",
    val isWithdrawDialogVisible: Boolean = false,
) : UiState

sealed interface AppSettingIntent : UiIntent {
    data object ClickBack : AppSettingIntent

    data object ClickAccount : AppSettingIntent

    data object ClickServiceTerms : AppSettingIntent

    data object ClickPrivacyPolicy : AppSettingIntent

    data object ClickLogout : AppSettingIntent

    data object ClickWithdraw : AppSettingIntent

    data object ConfirmWithdraw : AppSettingIntent

    data object DismissWithdrawDialog : AppSettingIntent
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
            AppSettingIntent.ClickLogout -> handleClickLogout()
            AppSettingIntent.ClickWithdraw -> handleClickWithdraw()
            AppSettingIntent.ConfirmWithdraw -> handleConfirmWithdraw()
            AppSettingIntent.DismissWithdrawDialog -> handleDismissWithdrawDialog()
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

    private fun handleClickLogout() {
        // TODO auth 로그아웃 연동 전 stub
        viewModelLogger.i { "AppSettingViewModel::handleClickLogout (stub)" }
    }

    private fun handleClickWithdraw() {
        updateState { copy(isWithdrawDialogVisible = true) }
    }

    private fun handleConfirmWithdraw() {
        updateState { copy(isWithdrawDialogVisible = false) }
        // TODO 회원 탈퇴 API 연동 전 stub
        viewModelLogger.i { "AppSettingViewModel::handleConfirmWithdraw (stub)" }
    }

    private fun handleDismissWithdrawDialog() {
        updateState { copy(isWithdrawDialogVisible = false) }
    }
}
