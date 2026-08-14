package com.teamyg.parfait.feature.login.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.usecase.auth.LoginWithKakaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
) : UiState

sealed interface LoginIntent : UiIntent {
    data object LoginWithKakao : LoginIntent

    /**
     * @param idToken 카카오 SDK 가 준 ID 토큰
     * @param nonce SDK 요청에 쓴 값. 서버에도 같은 값을 보낸다
     */
    data class LoginWithKakaoSuccess(
        val idToken: String,
        val nonce: String,
    ) : LoginIntent

    data class LoginWithKakaoFailure(val throwable: Throwable?) : LoginIntent

    data object LoginWithKakaoCancel : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    data object RequestLoginWithKakao : LoginSideEffect

    /** 신규 회원 — 약관 동의로 보낸다. 뒤로가기가 로그인으로 와야 하므로 백스택을 지우지 않는다 */
    data class NavigateToTermAgree(val registrationToken: String) : LoginSideEffect

    /** 기존 회원 — 세션 저장이 끝났다. 백스택을 지우고 그룹 목록으로 간다 */
    data object NavigateToGroupList : LoginSideEffect
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
            is LoginIntent.LoginWithKakao -> requestSdkLogin()

            is LoginIntent.LoginWithKakaoSuccess -> requestServerLogin(
                idToken = intent.idToken,
                nonce = intent.nonce,
            )

            is LoginIntent.LoginWithKakaoFailure -> {
                updateState { copy(isLoading = false) }
                // TODO(에러 UX 미정): 실패 안내 노출. idToken 이 null 이면 콘솔 OIDC 설정 문제다
                viewModelLogger.e(intent.throwable) { "카카오 SDK 로그인 실패" }
            }

            is LoginIntent.LoginWithKakaoCancel -> {
                updateState { copy(isLoading = false) }
                viewModelLogger.d { "사용자가 카카오 로그인을 취소했다" }
            }
        }
    }

    /**
     * SDK 다이얼로그는 [launch] 가드보다 앞에서 뜨므로 로딩 플래그로 한 겹 더 막는다.
     * 안 그러면 연타에 카카오 로그인 창이 두 번 뜬다.
     */
    private fun requestSdkLogin() {
        if (state.value.isLoading) {
            viewModelLogger.d { "로그인 진행 중이라 중복 요청을 무시한다" }
            return
        }
        updateState { copy(isLoading = true) }
        postSideEffect(LoginSideEffect.RequestLoginWithKakao)
    }

    private fun requestServerLogin(
        idToken: String,
        nonce: String,
    ) {
        launch(key = KEY_KAKAO_LOGIN) {
            try {
                loginWithKakaoUseCase(idToken = idToken, nonce = nonce)
                    .onSuccess(::navigateByMemberType)
                    .onFailure(::logServerLoginFailure)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun navigateByMemberType(result: KakaoLoginVO) {
        when (result) {
            is KakaoLoginVO.ExistingMember -> postSideEffect(LoginSideEffect.NavigateToGroupList)

            is KakaoLoginVO.NewUser -> postSideEffect(
                LoginSideEffect.NavigateToTermAgree(registrationToken = result.registrationToken.value),
            )
        }
    }

    /**
     * 실패 갈래를 전부 열거해 둔다. 지금은 로그뿐이지만, UX 가 정해지면 각 자리를 문구로
     * 바꾸면 되고 분기를 다시 발굴할 필요가 없다.
     */
    private fun logServerLoginFailure(throwable: Throwable) {
        when (throwable) {
            is AppError.Network ->
                // TODO(에러 UX 미정): "네트워크 연결을 확인해 주세요" + 재시도 안내
                viewModelLogger.e(throwable) { "로그인 실패 — 네트워크 단절" }

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.Auth.INVALID_ID_TOKEN ->
                    // TODO(에러 UX 미정): 다시 로그인 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — ID 토큰 검증 실패(401)" }

                ServerErrorCode.Auth.KAKAO_JWKS_FETCH_FAILED ->
                    // TODO(에러 UX 미정): 잠시 후 재시도 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 공개키 조회 실패(502)" }

                ServerErrorCode.Auth.KAKAO_SERVER_UNAVAILABLE ->
                    // TODO(에러 UX 미정): 잠시 후 재시도 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 서버 연결 불가(503)" }

                else ->
                    // TODO(에러 UX 미정): 알 수 없는 서버 에러 안내
                    viewModelLogger.e(throwable) { "로그인 실패 — 미분류 서버 에러 ${throwable.code}" }
            }

            else ->
                // TODO(에러 UX 미정): 알 수 없는 오류 안내. 매퍼 실패·파싱 실패가 여기로 온다
                viewModelLogger.e(throwable) { "로그인 실패 — 예상하지 못한 오류" }
        }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 이 ViewModel 의 서버 로그인 job 하나를 가리킨다 */
        const val KEY_KAKAO_LOGIN = "kakaoLogin"
    }
}
