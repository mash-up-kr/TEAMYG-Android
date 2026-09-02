package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.APP_VERSION_NAME
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import com.teamyg.parfait.domain.usecase.member.WithdrawUseCase
import com.teamyg.parfait.domain.usecase.policy.GetPoliciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property nickname 계정 정보 SSoT 구독 결과. `null` 은 아직 SSoT 가 값을 방출하지 않은
 *   로딩 상태를 뜻한다(빈 문자열이 아니다) — 화면은 이 값을 직접 갱신하지 않고 구독만 한다.
 * @property loginProvider 도메인 의미만 담는다(ADR-0016). 표시 문자열은 `core:ui`
 *   `LoginProvider.toStringResource()` 가 렌더 시점에 매핑한다. `null` 은 `nickname` 과 같은
 *   이유로 로딩이다.
 * @property version 앱 버전. 빌드에 박혀 있어 로딩 상태가 없다 — `nickname` 과 달리 처음부터
 *   값이 있어 non-null 이다
 * @property policies 약관 목록. 화면의 약관 두 줄은 문자열 리소스로 고정돼 있고 이 목록은
 *   **누를 때 열 제목과 주소의 출처로만** 쓴다 — 조회가 실패해도 줄이 사라지지 않아야 한다.
 *   비어 있으면 아직 못 받았거나 조회가 실패한 것이다
 * @property isWithdrawDialogVisible 서비스 탈퇴 확인 팝업 노출 여부
 * @property isLoggingOut 로그아웃 요청이 진행 중인지. 진행 중이면 로그아웃 버튼을 비활성한다
 */
data class AppSettingState(
    val nickname: String? = null,
    val loginProvider: LoginProvider? = null,
    val version: String = APP_VERSION_NAME,
    val policies: List<PolicyVO> = emptyList(),
    val isWithdrawDialogVisible: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isWithdrawing: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = isLoggingOut || isWithdrawing

    fun policyOf(type: PolicyType): PolicyVO? = policies.firstOrNull { it.type == type }
}

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

    data class NavigateToPolicyDetail(
        val title: String,
        val url: String,
    ) : AppSettingSideEffect

    data object NavigateToLogin : AppSettingSideEffect

    /** 탈퇴가 거절됐다. 계정이 그대로 살아 있으므로 화면을 떠나지 않고 알리기만 한다 */
    data object ShowWithdrawError : AppSettingSideEffect
}

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
    private val logout: LogoutUseCase,
    private val withdraw: WithdrawUseCase,
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
    private val getPolicies: GetPoliciesUseCase,
) : BaseViewModel<AppSettingState, AppSettingIntent, AppSettingSideEffect>(
    initialState = AppSettingState(),
) {
    init {
        viewModelLogger.i { "AppSettingViewModel::init" }

        // 구독만 한다 — fetch 는 로그인/가입·스플래시 부트스트랩·닉네임 변경 성공
        // 세 지점에서만 트리거된다. 여기서 새로고침을 걸면 SSoT 가 두 곳에서 쓰이게 된다.
        launch {
            getMyAccountFlow().collect { account ->
                updateState {
                    copy(
                        nickname = account?.nickname?.value,
                        loginProvider = account?.provider,
                    )
                }
            }
        }

        loadPolicies()
    }

    override fun processIntent(intent: AppSettingIntent) {
        when (intent) {
            AppSettingIntent.ClickBack -> handleClickBack()
            AppSettingIntent.ClickAccount -> handleClickAccount()
            AppSettingIntent.ClickServiceTerms -> handleClickPolicy(PolicyType.TERMS_OF_SERVICE)
            AppSettingIntent.ClickPrivacyPolicy -> handleClickPolicy(PolicyType.PRIVACY_POLICY)
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

    private fun loadPolicies() {
        launch(key = KEY_LOAD_POLICIES) {
            getPolicies()
                .onSuccess { policies -> updateState { copy(policies = policies) } }
                .onFailure { viewModelLogger.e(it) { "약관 목록 조회에 실패했다" } }
        }
    }

    /**
     * 찾는 약관이 없으면 아무것도 하지 않는다. 여기서 다시 조회하지 않는 이유: 목록에 그 종류가
     * 없다는 것은 서버가 잘못 내려줬다는 뜻이고, 같은 요청을 되풀이해도 같은 응답이 온다.
     * 탭마다 요청만 늘면서 결함은 로그에만 남아 가려진다.
     */
    private fun handleClickPolicy(type: PolicyType) {
        val policy = state.value.policyOf(type)

        if (policy == null) {
            // 받은 개수를 함께 남긴다 — 0 이면 조회가 아직 안 끝났거나 실패한 것이고,
            // 0 이 아니면 서버가 이 종류를 빼고 준 것이다
            viewModelLogger.w {
                "약관을 찾지 못해 열지 못했다 - type: $type, 받은 약관 수: ${state.value.policies.size}"
            }
            return
        }

        postSideEffect(
            AppSettingSideEffect.NavigateToPolicyDetail(title = policy.title, url = policy.url),
        )
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

        launch(key = KEY_WITHDRAW) {
            updateState { copy(isWithdrawing = true) }
            try {
                withdraw()
                    .onSuccess { postSideEffect(AppSettingSideEffect.NavigateToLogin) }
                    .onFailure { throwable ->
                        viewModelLogger.e(throwable) { "회원 탈퇴 실패" }
                        postSideEffect(AppSettingSideEffect.ShowWithdrawError)
                    }
            } finally {
                updateState { copy(isWithdrawing = false) }
            }
        }
    }

    private fun handleDismissWithdrawDialog() {
        updateState { copy(isWithdrawDialogVisible = false) }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 로그아웃 job 하나를 가리킨다 */
        const val KEY_LOGOUT = "logout"

        /** [launch] 중복 실행 가드 키 — 약관 조회 job 하나를 가리킨다 */
        const val KEY_LOAD_POLICIES = "loadPolicies"

        /** [launch] 중복 실행 가드 키 — 탈퇴 job 하나를 가리킨다 */
        const val KEY_WITHDRAW = "withdraw"
    }
}
