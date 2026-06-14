package com.teamyg.parfait.feature.splash.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.navigation.Navigator

@Composable
fun SplashRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    SplashScreen(
        modifier = modifier,
    )
}
