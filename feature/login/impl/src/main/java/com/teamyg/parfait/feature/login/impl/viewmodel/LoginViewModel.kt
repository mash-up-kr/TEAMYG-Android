package com.teamyg.parfait.feature.login.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.usecase.LoginWithKakaoUseCase
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val token: String? = null,
) : UiState

sealed interface LoginIntent : UiIntent {
    object LoginWithKakao : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    class NavigateToNext : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    init {
        viewModelLogger.i { "LoginViewModel::init" }
    }

    override fun processIntent(intent: LoginIntent) {
        when (intent) {
            LoginIntent.LoginWithKakao -> {
                viewModelScope.launch {
                    when (val result = loginWithKakaoUseCase()) {
                        is KakaoLoginResult.Success -> {
                            updateState { copy(token = result.token) }
                            viewModelLogger.d { "카카오 계정으로 로그인 성공 : ${result.token}" }
                            postSideEffect(LoginSideEffect.NavigateToNext())
                        }

                        is KakaoLoginResult.Failure -> {
                            viewModelLogger.e(result.throwable) { "카카오 계정으로 로그인 실패 : ${result.throwable}" }
                        }

                        is KakaoLoginResult.Cancel -> Unit
                    }
                }
            }
        }
    }
}
