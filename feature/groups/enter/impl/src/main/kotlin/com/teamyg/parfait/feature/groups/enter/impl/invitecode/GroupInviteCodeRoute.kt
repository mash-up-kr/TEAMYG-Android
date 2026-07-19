package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupInviteCodeRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupInviteCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    // Todo : 클립보드 확인 후 자동복붙하는 기능은 추후 추가 예정

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupInviteCodeSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupInviteCodeSideEffect.NavigateToNext -> { /* navigate to next */ }
            }
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible.not()) {
            viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
    }

    LaunchedEffect(uiState.focusedIndex) {
        if (uiState.focusedIndex == null) {
            keyboardController?.hide()
        } else {
            keyboardController?.show()
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
