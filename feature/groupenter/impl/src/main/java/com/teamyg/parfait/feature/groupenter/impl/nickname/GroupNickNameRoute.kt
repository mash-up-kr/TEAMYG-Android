package com.teamyg.parfait.feature.groupenter.impl.nickname

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
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

    GroupNickNameScreen(
        uiState = uiState,
        modifier = modifier.imePadding(),
    )
}
