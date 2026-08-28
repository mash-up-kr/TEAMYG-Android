package com.teamyg.parfait.feature.login.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.auth.KakaoLoginVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.repository.debug.DebugModeRepository
import com.teamyg.parfait.domain.usecase.auth.LoginWithKakaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val isDebugMode: Boolean = false,
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

    /** 로그인 화면 빈 영역 더블탭. 7회 뒤 롱프레스가 디버그 모드를 연다 */
    data object DebugDoubleTap : LoginIntent

    data object DebugLongPress : LoginIntent

    data object DisableDebugMode : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    /** @param forceAccountLogin 참이면 카카오톡을 건너뛰고 계정(웹) 로그인으로 간다 */
    data class RequestLoginWithKakao(val forceAccountLogin: Boolean) : LoginSideEffect

    /** 신규 회원 — 약관 동의로 보낸다. 뒤로가기가 로그인으로 와야 하므로 백스택을 지우지 않는다 */
    data class NavigateToTermAgree(val registrationToken: String) : LoginSideEffect

    /** 기존 회원 — 세션 저장이 끝났다. 백스택을 지우고 그룹 목록으로 간다 */
    data object NavigateToGroupList : LoginSideEffect

    /**
     * 실패를 알린다. 문구가 아니라 **사유**를 실어 보낸다 — 문자열 매핑은 프레젠테이션
     * 소관이고(ADR-0016), ViewModel 이 리소스 ID 를 들면 그 결정이 되돌아온다.
     */
    data class ShowError(val error: LoginError) : LoginSideEffect
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    private val debugModeRepository: DebugModeRepository,
) : BaseViewModel<LoginState, LoginIntent, LoginSideEffect>(initialState = LoginState()) {
    /** 화면을 벗어나면 사라져도 되는 값이라 상태에 두지 않는다 — 탭마다 리컴포지션을 돌릴 이유가 없다 */
    private var debugDoubleTapCount = 0

    init {
        viewModelLogger.i { "LoginViewModel::init" }
        observeDebugMode()
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
                // idToken 이 null 이면 콘솔 OIDC 설정 문제다 — 사용자에게는 구분이 무의미해
                // 카카오 쪽 문제로 묶어 알린다
                postSideEffect(LoginSideEffect.ShowError(LoginError.KAKAO_UNAVAILABLE))
                viewModelLogger.e(intent.throwable) { "카카오 SDK 로그인 실패" }
            }

            is LoginIntent.LoginWithKakaoCancel -> {
                updateState { copy(isLoading = false) }
                viewModelLogger.d { "사용자가 카카오 로그인을 취소했다" }
            }

            is LoginIntent.DebugDoubleTap -> debugDoubleTapCount++

            is LoginIntent.DebugLongPress -> enableDebugModeIfGestureMatched()

            is LoginIntent.DisableDebugMode -> setDebugMode(enabled = false)
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
        postSideEffect(LoginSideEffect.RequestLoginWithKakao(forceAccountLogin = state.value.isDebugMode))
    }

    private fun requestServerLogin(
        idToken: String,
        nonce: String,
    ) {
        // `Result.failure` 는 아래 `onFailure` 가 받고, UseCase 가 예외를 그대로 던지는 경로는
        // 이 가드가 받는다. 둘 중 하나만 알리면 "어떤 실패는 조용히 아무 일도 안 일어난다"가 된다
        launch(
            key = KEY_KAKAO_LOGIN,
            onError = { postSideEffect(LoginSideEffect.ShowError(LoginError.UNKNOWN)) },
        ) {
            try {
                loginWithKakaoUseCase(idToken = idToken, nonce = nonce)
                    .onSuccess(::navigateByMemberType)
                    .onFailure(::notifyServerLoginFailure)
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
     * 실패 갈래를 전부 열거해 로그를 남기고 사용자에게 알린다.
     *
     * 로그의 분기가 [LoginError] 보다 잘게 갈라져 있는 것은 의도다 — 개발자는 502 와 503 을
     * 구분해야 하지만 사용자는 둘 다 "잠시 후 다시"로 같다.
     */
    private fun notifyServerLoginFailure(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> {
                viewModelLogger.e(throwable) { "로그인 실패 — 네트워크 단절" }
                LoginError.NETWORK
            }

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.Auth.INVALID_ID_TOKEN -> {
                    viewModelLogger.e(throwable) { "로그인 실패 — ID 토큰 검증 실패(401)" }
                    LoginError.INVALID_ID_TOKEN
                }

                ServerErrorCode.Auth.KAKAO_JWKS_FETCH_FAILED -> {
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 공개키 조회 실패(502)" }
                    LoginError.KAKAO_UNAVAILABLE
                }

                ServerErrorCode.Auth.KAKAO_SERVER_UNAVAILABLE -> {
                    viewModelLogger.e(throwable) { "로그인 실패 — 카카오 서버 연결 불가(503)" }
                    LoginError.KAKAO_UNAVAILABLE
                }

                else -> {
                    viewModelLogger.e(throwable) { "로그인 실패 — 미분류 서버 에러 ${throwable.code}" }
                    LoginError.UNKNOWN
                }
            }

            else -> {
                // 매퍼 실패·파싱 실패가 여기로 온다
                viewModelLogger.e(throwable) { "로그인 실패 — 예상하지 못한 오류" }
                LoginError.UNKNOWN
            }
        }

        postSideEffect(LoginSideEffect.ShowError(error))
    }

    private fun observeDebugMode() {
        launch {
            debugModeRepository.isEnabled.collect { enabled ->
                updateState { copy(isDebugMode = enabled) }
            }
        }
    }

    /**
     * 롱프레스는 성공하든 못 하든 카운터를 되돌린다 — 실패한 시도가 다음 시도에 누적되면
     * 아무 데나 눌러도 열리는 제스처가 된다.
     */
    private fun enableDebugModeIfGestureMatched() {
        val matched = debugDoubleTapCount == DEBUG_GESTURE_DOUBLE_TAP_COUNT
        debugDoubleTapCount = 0

        if (!matched) {
            return
        }

        viewModelLogger.i { "디버그 모드를 켠다" }
        setDebugMode(enabled = true)
    }

    private fun setDebugMode(enabled: Boolean) {
        launch { debugModeRepository.setEnabled(enabled) }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 이 ViewModel 의 서버 로그인 job 하나를 가리킨다 */
        const val KEY_KAKAO_LOGIN = "kakaoLogin"

        const val DEBUG_GESTURE_DOUBLE_TAP_COUNT = 7
    }
}
