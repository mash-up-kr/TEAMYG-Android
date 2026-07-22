package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NicknameResult
import com.teamyg.parfait.domain.usecase.group.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AccountInfoUiState(
    val nickname: String = "대충지은랜덤닉네임",
    val nicknameError: NicknameResult.Error? = null,
) : UiState

sealed interface AccountInfoIntent : UiIntent {
    data class InputWord(val nickName: String) : AccountInfoIntent

    data object ClickBack : AccountInfoIntent

    data object ClickLogout : AccountInfoIntent

    data object ClickWithdraw : AccountInfoIntent
}

sealed interface AccountInfoSideEffect : UiSideEffect {
    data object NavigateBack : AccountInfoSideEffect
}

@HiltViewModel
class AccountInfoViewModel
@Inject
constructor(
    private val checkNameValid: CheckNameValidUseCase,
) : BaseViewModel<AccountInfoUiState, AccountInfoIntent, AccountInfoSideEffect>(
    initialState = AccountInfoUiState(),
) {
    init {
        viewModelLogger.i { "AccountInfoViewModel::init" }
    }

    override fun processIntent(intent: AccountInfoIntent) {
        when (intent) {
            is AccountInfoIntent.InputWord -> handleInputWord(intent.nickName)
            AccountInfoIntent.ClickBack -> handleClickBack()
            AccountInfoIntent.ClickLogout -> handleClickLogout()
            AccountInfoIntent.ClickWithdraw -> handleClickWithdraw()
        }
    }

    private fun handleInputWord(nickName: String) {
        val result = checkNameValid(nickName)
        updateState {
            copy(
                nickname = nickName,
                nicknameError = result as? NicknameResult.Error,
            )
        }
    }

    private fun handleClickBack() {
        postSideEffect(AccountInfoSideEffect.NavigateBack)
    }

    private fun handleClickLogout() {
        // TODO auth 로그아웃 연동 전 stub
        viewModelLogger.i { "AccountInfoViewModel::handleClickLogout (stub)" }
    }

    private fun handleClickWithdraw() {
        // TODO 회원 탈퇴 API 연동 전 stub
        viewModelLogger.i { "AccountInfoViewModel::handleClickWithdraw (stub)" }
    }
}
