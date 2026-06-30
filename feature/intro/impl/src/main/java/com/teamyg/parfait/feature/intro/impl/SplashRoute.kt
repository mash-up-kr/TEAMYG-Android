package com.teamyg.parfait.feature.intro.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.login.api.NavKeyLogin

@Composable
fun SplashRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.loadingStatus) {
        if (state.loadingStatus == LoadingStatus.Success) {
            navigator.clearBackStack()
            navigator.goTo(destination = NavKeyLogin)
        }
    }

    SplashScreen(
        modifier = modifier,
    )
}
