package com.teamyg.parfait.feature.login.impl.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelper
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelperFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val token: String? = null,
) : UiState

sealed interface LoginIntent : UiIntent {
    data class LoginWithKakao(val context: Context) : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    class NavigateToNext : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val kakaoLoginHelperFactory: KakaoLoginHelperFactory,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    override fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.LoginWithKakao -> {
                viewModelScope.launch {
                    val kakaoLoginHelper = kakaoLoginHelperFactory.create(intent.context)
                    val result = if (kakaoLoginHelper.isKakaoTalkLoginAvailable()) {
                        when (val result = kakaoLoginHelper.loginWithKakaoTalk()) {
                            is KakaoLoginResult.Success -> result
                            is KakaoLoginResult.Cancel -> result
                            is KakaoLoginResult.Failure -> kakaoLoginHelper.loginWithKakaoAccount()
                        }
                    } else {
                        kakaoLoginHelper.loginWithKakaoAccount()
                    }

                    when (result) {
                        is KakaoLoginResult.Success -> {
                            updateState { copy(token = result.token) }
                            Log.i(TAG, "카카오 계정으로 로그인 성공 : ${result.token}")
                            postSideEffect(LoginSideEffect.NavigateToNext())
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
