package com.teamyg.parfait.feature.login.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LoginState(
    val token: String? = null,
) : UiState

sealed interface LoginIntent : UiIntent {
    data object LoginWithKakao : LoginIntent

    data class LoginWithKakaoSuccess(val token: String) : LoginIntent

    data class LoginWithKakaoFailure(val throwable: Throwable?) : LoginIntent

    data object LoginWithKakaoCancel : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    class NavigateToNext : LoginSideEffect

    data object RequestLoginWithKakao : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor() : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    init {
        viewModelLogger.i { "LoginViewModel::init" }
    }

    override fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.LoginWithKakao -> {
                postSideEffect(LoginSideEffect.RequestLoginWithKakao)
            }

            is LoginIntent.LoginWithKakaoSuccess -> {
                updateState { copy(token = intent.token) }
                viewModelLogger.d { "카카오 계정으로 로그인 성공 : ${intent.token}" }
                postSideEffect(LoginSideEffect.NavigateToNext())
            }

            is LoginIntent.LoginWithKakaoFailure -> {
                viewModelLogger.e(intent.throwable) { "카카오 계정으로 로그인 실패 : ${intent.throwable}" }
            }

            is LoginIntent.LoginWithKakaoCancel -> {
                viewModelLogger.d { "카카오 계정 로그인 취소" }
            }
        }
    }
}
