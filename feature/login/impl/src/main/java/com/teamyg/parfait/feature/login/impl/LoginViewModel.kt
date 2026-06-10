package com.teamyg.parfait.feature.login.impl

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.teamyg.domain.model.KakaoLoginResult
import com.teamyg.domain.usecase.LoginWithKakaoUseCase
import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val token: String? = null,
) : UiState

sealed interface LoginIntent : UiIntent {
    object LoginWithKakao : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    override fun processIntent(intent: LoginIntent) {
        when (intent) {
            LoginIntent.LoginWithKakao -> {
                viewModelScope.launch {
                    when (val result = loginWithKakaoUseCase()) {
                        is KakaoLoginResult.Success -> {
                            updateState { copy(token = result.token) }
                            Log.i(TAG, "카카오 계정으로 로그인 성공 : ${result.token}")
                        }

                        is KakaoLoginResult.Failure -> {
                            Log.e(TAG, "카카오 계정으로 로그인 실패 : ${result.throwable}")
                        }

                        is KakaoLoginResult.Cancel -> Unit
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "LoginViewModel"
    }
}
