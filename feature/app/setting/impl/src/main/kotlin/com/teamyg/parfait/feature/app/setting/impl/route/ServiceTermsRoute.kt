package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.ServiceTermsScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsViewModel

@Composable
internal fun ServiceTermsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ServiceTermsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ServiceTermsSideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    ServiceTermsScreen(
        url = state.url,
        onClickBack = { viewModel.processIntent(ServiceTermsIntent.ClickBack) },
        modifier = modifier,
    )
}
