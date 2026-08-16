package com.teamyg.parfait.feature.intro.impl.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.login.api.NavKeyLogin

@Composable
fun SplashRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SplashSideEffect.NavigateToLogin -> navigator.replaceAll(destination = NavKeyLogin)
                SplashSideEffect.NavigateToGroupList -> navigator.replaceAll(destination = NavKeyGroupList)
            }
        }
    }

    SplashScreen(
        modifier = modifier,
    )
}
