package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class AccountInfoUiState(
    val nickname: String = "대충지은랜덤닉네임",
    val nicknameError: NameValidResult.Error? = null,
) : UiState

sealed interface AccountInfoIntent : UiIntent {
    data class InputWord(val nickName: String) : AccountInfoIntent

    data object ClickBack : AccountInfoIntent
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
        }
    }

    private fun handleInputWord(nickName: String) {
        val nicknameError = checkNameValid(nickName) as? NameValidResult.Error

        updateState {
            copy(
                nickname = nickName,
                nicknameError = nicknameError,
            )
        }
    }

    private fun handleClickBack() {
        postSideEffect(AccountInfoSideEffect.NavigateBack)
    }
}
