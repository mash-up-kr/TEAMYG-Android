package com.teamyg.parfait.feature.intro.impl.termagree

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.exception.SignUpException
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.usecase.auth.SignUpUseCase
import com.teamyg.parfait.domain.usecase.policy.GetPoliciesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class TermAgreeState(
    val policies: List<PolicyVO> = emptyList(),
    val agreedTermsIds: Set<TermsId> = emptySet(),
    val isLoading: Boolean = true,
    /** 재시도라는 갈 곳이 있는 실패라 토스트로 흘려보내지 않고 화면에 남긴다 */
    val isLoadFailed: Boolean = false,
    val isSigningUp: Boolean = false,
) : UiState {
    val isAllSelected: Boolean = policies.isNotEmpty() && policies.all { it.termsId in agreedTermsIds }

    val isAvailable: Boolean = policies.isNotEmpty() &&
        policies.none { it.required && it.termsId !in agreedTermsIds }

    fun isAgreed(policy: PolicyVO): Boolean = policy.termsId in agreedTermsIds
}

sealed interface TermAgreeIntent : UiIntent {
    data class ClickTermAgree(val termsId: TermsId, val newSelected: Boolean) : TermAgreeIntent

    /** 약관 줄의 화살표를 눌러 전문을 펼치려는 것. 동의 체크와는 별개다 */
    data class ClickTermDetail(val policy: PolicyVO) : TermAgreeIntent

    data class ClickAgreeAllTerm(val newSelected: Boolean) : TermAgreeIntent

    data object ClickNextButton : TermAgreeIntent

    data object ClickBackButton : TermAgreeIntent

    data object ClickRetryLoad : TermAgreeIntent
}

sealed interface TermAgreeSideEffect : UiSideEffect {
    /** 약관 전문 화면으로. 설정 화면과 같은 목적지를 쓴다 — 종류별로 화면을 나누지 않는다 */
    data class NavigateToPolicyDetail(
        val title: String,
        val url: String,
    ) : TermAgreeSideEffect

    data object NavigateToBack : TermAgreeSideEffect

    data object NavigateToNext : TermAgreeSideEffect

    data class ShowError(val error: TermAgreeError) : TermAgreeSideEffect
}

@HiltViewModel(assistedFactory = TermAgreeViewModel.Factory::class)
class TermAgreeViewModel
@AssistedInject
constructor(
    @Assisted registrationTokenValue: String,
    private val getPolicies: GetPoliciesUseCase,
    private val signUp: SignUpUseCase,
) : BaseViewModel<TermAgreeState, TermAgreeIntent, TermAgreeSideEffect>(initialState = TermAgreeState()) {
    private val registrationToken = RegistrationToken(registrationTokenValue)

    init {
        viewModelLogger.i { "TermAgreeViewModel::init" }
        loadPolicies()
    }

    override fun processIntent(intent: TermAgreeIntent) {
        when (intent) {
            is TermAgreeIntent.ClickAgreeAllTerm -> {
                updateState {
                    copy(
                        agreedTermsIds = if (intent.newSelected) {
                            policies.map(PolicyVO::termsId).toSet()
                        } else {
                            emptySet()
                        },
                    )
                }
            }

            is TermAgreeIntent.ClickTermAgree -> {
                updateState {
                    copy(
                        agreedTermsIds = if (intent.newSelected) {
                            agreedTermsIds + intent.termsId
                        } else {
                            agreedTermsIds - intent.termsId
                        },
                    )
                }
            }

            is TermAgreeIntent.ClickTermDetail ->
                postSideEffect(
                    TermAgreeSideEffect.NavigateToPolicyDetail(
                        title = intent.policy.title,
                        url = intent.policy.url,
                    ),
                )

            is TermAgreeIntent.ClickBackButton -> postSideEffect(TermAgreeSideEffect.NavigateToBack)

            is TermAgreeIntent.ClickNextButton -> requestSignUp()

            is TermAgreeIntent.ClickRetryLoad -> loadPolicies()
        }
    }

    /**
     * 로딩 플래그를 [launch] 밖에서 켜는 이유: 재시도 클릭이 [KEY_LOAD_POLICIES] 가드에
     * 막혀도 화면은 "조회 중"으로 보여야 하고, 실제로도 조회가 돌고 있다.
     */
    private fun loadPolicies() {
        updateState { copy(isLoading = true, isLoadFailed = false) }

        launch(key = KEY_LOAD_POLICIES) {
            try {
                getPolicies()
                    .onSuccess(::applyPolicies)
                    .onFailure(::handleLoadFailure)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 스피너가
                // 영영 도는 것을 여기서 막는다
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun applyPolicies(policies: List<PolicyVO>) {
        updateState {
            copy(
                policies = policies,
                // 목록이 바뀌면 사라진 약관의 동의 상태는 버린다
                agreedTermsIds = agreedTermsIds.intersect(policies.map(PolicyVO::termsId).toSet()),
            )
        }
    }

    /**
     * 중복 요청은 [KEY_SIGN_UP] 가드가 막는다. `isSigningUp` 은 버튼을 잠그는 표시일 뿐이라
     * 가드로 쓰지 않는다 — 두 곳에서 막으면 한쪽이 어긋났을 때 원인을 찾기 어렵다.
     */
    private fun requestSignUp() {
        val current = state.value
        if (current.isAvailable.not()) {
            viewModelLogger.d { "필수 약관 미동의 상태라 가입 요청을 보내지 않는다" }
            return
        }

        launch(key = KEY_SIGN_UP) {
            updateState { copy(isSigningUp = true) }
            try {
                signUp(
                    registrationToken = registrationToken,
                    policies = current.policies,
                    agreedTermsIds = current.agreedTermsIds,
                ).onSuccess { postSideEffect(TermAgreeSideEffect.NavigateToNext) }
                    .onFailure(::handleSignUpFailure)
            } finally {
                updateState { copy(isSigningUp = false) }
            }
        }
    }

    private fun handleLoadFailure(throwable: Throwable) {
        updateState { copy(isLoadFailed = true) }

        when (throwable) {
            is AppError.Network -> viewModelLogger.e(throwable) { "약관 목록 조회 실패 — 네트워크 단절" }

            is AppError.Server ->
                viewModelLogger.e(throwable) { "약관 목록 조회 실패 — 서버 에러 ${throwable.code}" }

            else -> viewModelLogger.e(throwable) { "약관 목록 조회 실패 — 예상하지 못한 오류" }
        }
    }

    /** 재시도 동선을 따로 주지 않는다 — 화면이 그대로 남아 다음 버튼이 그 자리에 있다 */
    private fun handleSignUpFailure(throwable: Throwable) {
        val error = when (throwable) {
            is SignUpException.RequiredPolicyNotAgreed -> {
                // 화면 가드(`isAvailable`)가 뚫렸다는 뜻이라 로그에는 결함으로 남긴다
                viewModelLogger.e(throwable) { "가입 실패 — 필수 약관 미동의로 도메인에서 막혔다" }
                TermAgreeError.UNKNOWN
            }

            is AppError.Network -> {
                viewModelLogger.e(throwable) { "가입 실패 — 네트워크 단절" }
                TermAgreeError.NETWORK
            }

            is AppError.Server -> {
                viewModelLogger.e(throwable) { "가입 실패 — 서버 에러 ${throwable.code}" }
                TermAgreeError.UNKNOWN
            }

            else -> {
                // 세션 저장 실패도 여기로 온다
                viewModelLogger.e(throwable) { "가입 실패 — 예상하지 못한 오류" }
                TermAgreeError.UNKNOWN
            }
        }

        postSideEffect(TermAgreeSideEffect.ShowError(error))
    }

    @AssistedFactory
    interface Factory {
        fun create(registrationTokenValue: String): TermAgreeViewModel
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 약관 조회 job 하나를 가리킨다 */
        const val KEY_LOAD_POLICIES = "loadPolicies"

        /** [launch] 중복 실행 가드 키 — 가입 요청 job 하나를 가리킨다 */
        const val KEY_SIGN_UP = "signUp"
    }
}
