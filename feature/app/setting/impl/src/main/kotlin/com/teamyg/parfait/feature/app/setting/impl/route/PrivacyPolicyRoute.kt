package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.PrivacyPolicyScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicyIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicySideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicyViewModel

@Composable
internal fun PrivacyPolicyRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: PrivacyPolicyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PrivacyPolicySideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    PrivacyPolicyScreen(
        url = state.url,
        onClickBack = { viewModel.processIntent(PrivacyPolicyIntent.ClickBack) },
        modifier = modifier,
    )
}
