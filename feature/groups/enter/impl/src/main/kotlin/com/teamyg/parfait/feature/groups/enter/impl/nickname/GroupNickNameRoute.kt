package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator

@Composable
fun GroupNickNameRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupNickNameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupNickNameSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupNickNameSideEffect.NavigateToNext -> { /* navigate to next */ }
            }
        }
    }

    GroupNickNameScreen(
        uiState = uiState,
        onValueChanged = { word -> viewModel.processIntent(GroupNickNameIntent.InputWord(word)) },
        onClickNextButton = { viewModel.processIntent(GroupNickNameIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupNickNameIntent.ClickBackButton) },
        modifier = modifier,
    )
}
