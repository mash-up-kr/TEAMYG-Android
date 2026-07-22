package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.AccountInfoScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoViewModel

@Composable
internal fun AccountInfoRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AccountInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountInfoSideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    AccountInfoScreen(
        state = state,
        onValueChanged = { viewModel.processIntent(AccountInfoIntent.InputWord(it)) },
        onClickBack = { viewModel.processIntent(AccountInfoIntent.ClickBack) },
        onClickLogout = { viewModel.processIntent(AccountInfoIntent.ClickLogout) },
        onClickWithdraw = { viewModel.processIntent(AccountInfoIntent.ClickWithdraw) },
        modifier = modifier,
    )
}
