package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.common.terms.api.NavKeyPrivacyPolicy
import com.teamyg.parfait.feature.common.terms.api.NavKeyServiceTerms
import com.teamyg.parfait.feature.login.api.NavKeyLogin
import com.teamyg.parfait.feature.app.setting.impl.screen.AppSettingScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingViewModel

@Composable
internal fun AppSettingRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AppSettingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AppSettingSideEffect.NavigateBack -> navigator.onBack()

                AppSettingSideEffect.NavigateToAccountInfo -> navigator.goTo(NavKeyAccountInfo)

                AppSettingSideEffect.NavigateToServiceTerms -> navigator.goTo(NavKeyServiceTerms)

                AppSettingSideEffect.NavigateToPrivacyPolicy -> navigator.goTo(NavKeyPrivacyPolicy)

                AppSettingSideEffect.NavigateToLogin -> {
                    navigator.replaceAll(NavKeyLogin)
                }
            }
        }
    }

    // 로그아웃 중에는 화면 전체를 막는다 — 끝나면 이 화면 자체가 `replaceAll` 로 사라지므로
    // 그사이 다른 설정 항목을 누르는 것은 의미가 없다. 로그아웃 버튼만 비활성하는 기존
    // 가드(`AppSettingState.isLoggingOut`)는 그대로 두고 그 위에 얹는다
    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoggingOut,
    ) { innerPadding ->
        AppSettingScreen(
            state = state,
            onClickBack = { viewModel.processIntent(AppSettingIntent.ClickBack) },
            onClickAccount = { viewModel.processIntent(AppSettingIntent.ClickAccount) },
            onClickTerms = { viewModel.processIntent(AppSettingIntent.ClickServiceTerms) },
            onClickPrivacy = { viewModel.processIntent(AppSettingIntent.ClickPrivacyPolicy) },
            onClickLogout = { viewModel.processIntent(AppSettingIntent.ClickLogout) },
            onClickWithdraw = { viewModel.processIntent(AppSettingIntent.ClickWithdraw) },
            onConfirmWithdraw = { viewModel.processIntent(AppSettingIntent.ConfirmWithdraw) },
            onDismissWithdrawDialog = {
                viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
