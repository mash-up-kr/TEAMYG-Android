package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.common.terms.api.NavKeyPrivacyPolicy
import com.teamyg.parfait.feature.common.terms.api.NavKeyServiceTerms
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
            }
        }
    }

    AppSettingScreen(
        state = state,
        onClickBack = { viewModel.processIntent(AppSettingIntent.ClickBack) },
        onClickAccount = { viewModel.processIntent(AppSettingIntent.ClickAccount) },
        onClickTerms = { viewModel.processIntent(AppSettingIntent.ClickServiceTerms) },
        onClickPrivacy = { viewModel.processIntent(AppSettingIntent.ClickPrivacyPolicy) },
        onClickLogout = { viewModel.processIntent(AppSettingIntent.ClickLogout) },
        onClickWithdraw = { viewModel.processIntent(AppSettingIntent.ClickWithdraw) },
        modifier = modifier,
    )
}
