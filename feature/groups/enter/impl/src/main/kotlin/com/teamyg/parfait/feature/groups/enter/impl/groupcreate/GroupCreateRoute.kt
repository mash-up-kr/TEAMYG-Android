package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupCreateRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupCreateSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupCreateSideEffect.NavigateToNext -> { /* navigate to next */ }
            }
        }
    }

    GroupCreateScreen(
        uiState = uiState,
        onClickNextButton = { viewModel.processIntent(GroupCreateIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupCreateIntent.ClickBackButton) },
        onGroupNameChanged = { viewModel.processIntent(GroupCreateIntent.InputGroupName(it)) },
        onClickGroupNumber = { viewModel.processIntent(GroupCreateIntent.ClickGroupNumber(it)) },
        modifier = modifier,
    )
}
