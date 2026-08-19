package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            }
        }
    }

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
        modifier = modifier,
    )
}
