package com.teamyg.parfait.feature.groupenter.impl.invitecode

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator

@Composable
fun GroupInviteCodeRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupInviteCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupInviteCodeSideEffect.NavigateToBack -> { navigator.onBack() }
                GroupInviteCodeSideEffect.NavigateToNext -> { /* navigate to next */ }
            }
        }
    }

    GroupInviteCodeScreen(
        uiState = uiState,
        onValueChanged = { index, word -> viewModel.processIntent(GroupInviteCodeIntent.InputWord(index, word)) },
        onClickTextFieldElement = { index ->
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index))
        },
        onClickNextButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickBackButton) },
        modifier = modifier.imePadding(),
    )
}
