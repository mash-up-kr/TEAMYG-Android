package com.teamyg.parfait.feature.intro.impl.splash

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
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

    YGScaffoldV2(
        modifier = modifier,
        // 로띠는 시스템바를 비켜난 영역이 아니라 화면의 정중앙에 있어야 한다
        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0),
    ) { innerPadding ->
        SplashScreen(
            onAnimationFinished = { viewModel.processIntent(SplashIntent.AnimationFinished) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
