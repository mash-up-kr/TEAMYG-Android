package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.common.terms.api.NavKeyWebView
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
    val toastPolicy = rememberYGToastPolicy()
    val withdrawErrorMessage = stringResource(R.string.setting_withdraw_error)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AppSettingSideEffect.NavigateBack -> navigator.onBack()

                AppSettingSideEffect.NavigateToAccountInfo -> navigator.goTo(NavKeyAccountInfo)

                is AppSettingSideEffect.NavigateToPolicyDetail -> navigator.goTo(
                    NavKeyWebView(title = effect.title, url = effect.url),
                )

                AppSettingSideEffect.NavigateToLogin -> {
                    navigator.replaceAll(NavKeyLogin)
                }

                AppSettingSideEffect.ShowWithdrawError -> {
                    toastPolicy.showError(withdrawErrorMessage)
                }
            }
        }
    }

    // 로그아웃·탈퇴 중에는 화면 전체를 막는다 — 성공하면 이 화면 자체가 `replaceAll` 로
    // 사라지므로 그사이 다른 설정 항목을 누르는 것은 의미가 없다
    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isLoading,
        toastPolicy = toastPolicy,
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
