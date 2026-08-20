package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.feature.common.terms.api.NavKeyWebView
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList

@Composable
fun TermAgreeRoute(
    navigator: Navigator,
    registrationToken: RegistrationToken,
    modifier: Modifier = Modifier,
    viewModel: TermAgreeViewModel = hiltViewModel<TermAgreeViewModel, TermAgreeViewModel.Factory>(
        creationCallback = { factory -> factory.create(registrationToken.value) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트 수집은 코루틴이라 그 안에서 `stringResource` 를 부를 수 없다 — 미리 뽑아 둔다
    val errorMessages = TermAgreeError.entries.associateWith { it.toStringResource() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is TermAgreeSideEffect.NavigateToPolicyDetail -> {
                    navigator.goTo(NavKeyWebView(title = it.title, url = it.url))
                }

                is TermAgreeSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is TermAgreeSideEffect.NavigateToNext -> {
                    navigator.replaceAll(destination = NavKeyGroupList)
                }

                is TermAgreeSideEffect.ShowError -> {
                    toastPolicy.showError(errorMessages.getValue(it.error))
                }
            }
        }
    }

    // 가입 요청도 덮는다 — 다음 버튼은 `isSigningUp` 을 보지 않아 응답을 기다리는 동안에도 눌린다
    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoading || state.isSigningUp,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        TermAgreeScreen(
            state = state,
            onClickTermAgree = { termsId, newSelected ->
                viewModel.processIntent(TermAgreeIntent.ClickTermAgree(termsId, newSelected))
            },
            onClickTermDetail = { viewModel.processIntent(TermAgreeIntent.ClickTermDetail(it)) },
            onClickAgreeAllTerm = { viewModel.processIntent(TermAgreeIntent.ClickAgreeAllTerm(it)) },
            onClickNextButton = { viewModel.processIntent(TermAgreeIntent.ClickNextButton) },
            onClickBackButton = { viewModel.processIntent(TermAgreeIntent.ClickBackButton) },
            onClickRetryLoad = { viewModel.processIntent(TermAgreeIntent.ClickRetryLoad) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
