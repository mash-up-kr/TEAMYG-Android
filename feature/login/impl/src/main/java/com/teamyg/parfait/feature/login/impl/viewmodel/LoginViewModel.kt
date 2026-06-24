package com.teamyg.parfait.feature.login.impl.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.usecase.LoginWithKakaoUseCase
import com.teamyg.parfait.core.ui.mvi.ContainerHost
import com.teamyg.parfait.core.ui.mvi.extension.container
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LoginState(
    val token: String? = null,
)

sealed interface LoginSideEffect {
    class NavigateToNext : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
) : ViewModel(), ContainerHost<LoginState, LoginSideEffect> {
    override val container = container<LoginState, LoginSideEffect>(
        initialState = LoginState(),
    )

    fun loginKakao() = intent {
        when (val result = loginWithKakaoUseCase()) {
            is KakaoLoginResult.Success -> {
                reduce { copy(token = result.token) }
                Log.i(TAG, "카카오 계정으로 로그인 성공 : ${result.token}")
                postSideEffect(LoginSideEffect.NavigateToNext())
            }

            is KakaoLoginResult.Failure -> {
                Log.e(TAG, "카카오 계정으로 로그인 실패 : ${result.throwable}")
            }

            is KakaoLoginResult.Cancel -> Unit
        }
    }

    companion object {
        const val TAG = "LoginViewModel"
    }
}
