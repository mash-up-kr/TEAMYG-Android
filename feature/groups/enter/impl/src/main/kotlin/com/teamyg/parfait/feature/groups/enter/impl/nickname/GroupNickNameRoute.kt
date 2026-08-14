package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList

@Composable
fun GroupNickNameRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupNickNameViewModel,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupNickNameSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupNickNameSideEffect.NavigateToNext -> {
                    navigator.goToSingleClearTop(destination = NavKeyGroupList)
                }
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
