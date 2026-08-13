package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.domain.usecase.auth.SignUpUseCase
import com.teamyg.parfait.domain.usecase.policy.GetPoliciesUseCase
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeSideEffect.NavigateToBack
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeSideEffect.NavigateToNext
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeSideEffect.NavigateToUrl
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

data class TermAgreeState(
    val policies: List<PolicyVO> = emptyList(),
    val agreedTermsIds: Set<TermsId> = emptySet(),
    val isLoading: Boolean = true,
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

    data class ClickTermLandingUrl(val landingUrl: String) : TermAgreeIntent

    data class ClickAgreeAllTerm(val newSelected: Boolean) : TermAgreeIntent

    data object ClickNextButton : TermAgreeIntent

    data object ClickBackButton : TermAgreeIntent

    data object ClickRetryLoad : TermAgreeIntent
}

sealed interface TermAgreeSideEffect : UiSideEffect {
    data class NavigateToUrl(val landingUrl: String) : TermAgreeSideEffect

    data object NavigateToBack : TermAgreeSideEffect

    data object NavigateToNext : TermAgreeSideEffect
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
                        agreedTermsIds = when (intent.newSelected) {
                            true -> policies.map(PolicyVO::termsId).toSet()
                            false -> emptySet()
                        },
                    )
                }
            }

            is TermAgreeIntent.ClickTermAgree -> {
                updateState {
                    copy(
                        agreedTermsIds = when (intent.newSelected) {
                            true -> agreedTermsIds + intent.termsId
                            false -> agreedTermsIds - intent.termsId
                        },
                    )
                }
            }

            is TermAgreeIntent.ClickTermLandingUrl -> {
                postSideEffect(NavigateToUrl(intent.landingUrl))
            }

            TermAgreeIntent.ClickBackButton -> {
                postSideEffect(NavigateToBack)
            }

            TermAgreeIntent.ClickNextButton -> {
                handleClickNextButton()
            }

            TermAgreeIntent.ClickRetryLoad -> {
                loadPolicies()
            }
        }
    }

    private fun loadPolicies() {
        updateState { copy(isLoading = true, isLoadFailed = false) }

        viewModelScope.launch {
            getPolicies()
                .onSuccess { policies ->
                    updateState {
                        copy(
                            policies = policies,
                            // 목록이 바뀌면 사라진 약관의 동의 상태는 버린다
                            agreedTermsIds = agreedTermsIds.intersect(policies.map(PolicyVO::termsId).toSet()),
                            isLoading = false,
                        )
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "약관 목록 조회 실패" }
                    updateState { copy(isLoading = false, isLoadFailed = true) }
                }
        }
    }

    private fun handleClickNextButton() {
        val current = state.value
        if (current.isAvailable.not() || current.isSigningUp) {
            return
        }

        updateState { copy(isSigningUp = true) }
        viewModelScope.launch {
            signUp(
                registrationToken = registrationToken,
                policies = current.policies,
                agreedTermsIds = current.agreedTermsIds,
            ).onSuccess {
                updateState { copy(isSigningUp = false) }
                postSideEffect(NavigateToNext)
            }.onFailure { throwable ->
                viewModelLogger.e(throwable) { "회원 가입 실패" }
                updateState { copy(isSigningUp = false) }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(registrationTokenValue: String): TermAgreeViewModel
    }
}
