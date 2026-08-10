package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupCreate
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
                    navigator.goTo(NavKeyGroupCreate(nickName = uiState.nickName))
                }

                GroupListSideEffect.NavigateToInviteCode -> {
                    navigator.goTo(NavKeyGroupInviteCode)
                }
            }
        }
    }

    // 상단 인셋은 YGTopBarEmpty 가 직접 흡수하므로 Scaffold 는 하단/좌우 인셋만 내려준다.
    YGScaffold(
        containerColor = YGAtomicColors.Gray.Transparent,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) { innerPadding ->
        // 두 화면 모두 LazyColumn 만 GroupListPullToRefreshBox 로 감싸 pull-to-refresh 동작이 동일하다.
        if (uiState.isError) {
            GroupListErrorScreen(
                uiState = uiState,
                onClickSideMenu = { viewModel.processIntent(GroupListIntent.ClickSideMenu) },
                onRefresh = { viewModel.processIntent(GroupListIntent.Refresh) },
                modifier = modifier.padding(innerPadding),
            )
        } else {
            GroupListScreen(
                uiState = uiState,
                onClickChip = { viewModel.processIntent(GroupListIntent.ClickTopBarChip) },
                onClickSideMenu = { viewModel.processIntent(GroupListIntent.ClickSideMenu) },
                onClickTopping = { viewModel.processIntent(GroupListIntent.ClickTopping) },
                onRefresh = { viewModel.processIntent(GroupListIntent.Refresh) },
                modifier = modifier.padding(innerPadding),
            )
        }
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
