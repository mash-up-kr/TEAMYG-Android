package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode

@Composable
internal fun GroupListRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupListSideEffect.NavigateToAppSideMenu -> {
                    navigator.goTo(NavKeyAppSetting)
                }

                GroupListSideEffect.NavigateToCanvas -> {
                    // Todo : canvas page 이동
                }

                GroupListSideEffect.NavigateToCreateGroup -> {
                    // Todo : navigator.goTo(NavKeyGroupCreate)
                }

                GroupListSideEffect.NavigateToInviteCode -> {
                    navigator.goTo(NavKeyGroupInviteCode)
                }
            }
        }
    }

    // Todo : Error 화면 추가

    YGScaffold(containerColor = YGAtomicColors.Gray.Transparent) { innerPadding ->
        GroupListScreen(
            uiState = uiState,
            onClickChip = { viewModel.processIntent(GroupListIntent.ClickTopBarChip) },
            onClickSideMenu = { viewModel.processIntent(GroupListIntent.ClickSideMenu) },
            onClickTopping = { viewModel.processIntent(GroupListIntent.ClickTopping) },
            modifier = modifier.padding(innerPadding),
        )
    }

    if (uiState.groupAddButtonSelected) {
        YGScaffold(containerColor = YGAtomicColors.Transparency.Black25) { innerPadding ->
            GroupListAddGroupScreen(
                onClickCreateNewGroup = { viewModel.processIntent(GroupListIntent.ClickCreateNewGroup) },
                onClickEnterNewGroup = { viewModel.processIntent(GroupListIntent.ClickEnterNewGroup) },
                onDismissed = { viewModel.processIntent(GroupListIntent.DismissedTopBarChip) },
                modifier = modifier.padding(innerPadding),
            )
        }
    }
}
