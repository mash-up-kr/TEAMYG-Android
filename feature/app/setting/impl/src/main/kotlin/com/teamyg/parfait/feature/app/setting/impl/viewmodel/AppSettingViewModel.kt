package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property nickname TODO 프로필 API 연동 전 placeholder 데이터
 * @property loginProvider
 * @property version TODO BuildConfig.VERSION_NAME 주입으로 교체
 * @property isWithdrawDialogVisible 서비스 탈퇴 확인 팝업 노출 여부
 * @property isLoggingOut 로그아웃 요청이 진행 중인지. 진행 중이면 로그아웃 버튼을 비활성한다
 */
data class AppSettingState(
    val nickname: String = "아니야나그런데기니야",
    val loginProvider: String = "Kakao",
    val version: String = "1.0v",
    val isWithdrawDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
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

    data object NavigateToLogin : AppSettingSideEffect
}

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
    private val logout: LogoutUseCase,
) : BaseViewModel<AppSettingState, AppSettingIntent, AppSettingSideEffect>(
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
        launch(key = KEY_LOGOUT) {
            // `launch(key)` 는 두 번째 탭을 삼킬 뿐 버튼은 눌리는 것처럼 보인다.
            // 요청 중 비활성은 스펙 요구라 상태로 드러낸다.
            updateState { copy(isLoggingOut = true) }
            try {
                // logout() 은 서버 실패도 성공으로 접어 돌려준다 — 이 기기에서 나가는 것이
                // 사용자가 누른 것의 의미이고, 화면이 갈래를 나눌 이유가 없다
                logout()
                postSideEffect(AppSettingSideEffect.NavigateToLogin)
            } finally {
                // 예외·취소로 빠져나가도 버튼이 영구 비활성으로 남지 않게 한다
                updateState { copy(isLoggingOut = false) }
            }
        }
    }

    private fun handleClickWithdraw() {
        updateState { copy(isWithdrawDialogVisible = true) }
    }

    private fun handleConfirmWithdraw() {
        if (!state.value.isWithdrawDialogVisible) return

        updateState { copy(isWithdrawDialogVisible = false) }
        // TODO 회원 탈퇴 API 연동 — 서버에 해당 엔드포인트 계약이 아직 없다(신설 후 연동)
        viewModelLogger.i { "AppSettingViewModel::handleConfirmWithdraw (stub)" }
    }

    private fun handleDismissWithdrawDialog() {
        updateState { copy(isWithdrawDialogVisible = false) }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 로그아웃 job 하나를 가리킨다 */
        const val KEY_LOGOUT = "logout"
    }
}
