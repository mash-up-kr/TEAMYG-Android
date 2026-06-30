package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator

@Composable
fun TermAgreeRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: TermAgreeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TermAgreeScreen(
        state = state,
        modifier = modifier,
    )
}
