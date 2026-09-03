package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupCreate
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode

@Composable
internal fun GroupListRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // 백스택 아래에 깔린 엔트리는 컴포지션에서 빠지므로 다시 앞에 설 때 한 번 더 돈다.
    // 매번 다시 묻는 이유는 GroupListIntent.Enter 에 있다
    LifecycleResumeEffect(Unit) {
        viewModel.processIntent(GroupListIntent.Enter)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GroupListSideEffect.NavigateToAppSideMenu -> {
                    navigator.goTo(NavKeyAppSetting)
                }

                is GroupListSideEffect.NavigateToCanvas -> {
                    navigator.goTo(NavKeyCanvasMain(groupId = effect.groupId.value))
                }

                is GroupListSideEffect.NavigateToCreateGroup -> {
                    navigator.goTo(NavKeyGroupCreate(nickName = effect.nickName))
                }

                is GroupListSideEffect.NavigateToInviteCode -> {
                    navigator.goTo(NavKeyGroupInviteCode)
                }
            }
        }
    }

    var toppingsVisible by remember { mutableStateOf(true) }

    // 상단 인셋은 YGTopBarEmpty 가 직접 흡수하므로 Scaffold 는 하단/좌우 인셋만 내려준다.
    YGScaffoldV2(
        containerColor = YGAtomicColors.Gray.Transparent,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        // 에러 화면은 목록을 아예 안 그려 게이트를 풀어 줄 쪽이 없다 — 덮개가 남으면 안 된다
        isLoading = uiState.isInitialLoading || (!uiState.isError && !toppingsVisible),
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
                onClickTopping = { groupId -> viewModel.processIntent(GroupListIntent.ClickTopping(groupId)) },
                onRefresh = { viewModel.processIntent(GroupListIntent.Refresh) },
                onToppingsVisibleChange = { toppingsVisible = it },
                modifier = modifier.padding(innerPadding),
            )
        }
    }

    if (uiState.groupAddButtonSelected) {
        YGScaffoldV2(containerColor = YGAtomicColors.Transparency.Black25) { innerPadding ->
            GroupListAddGroupScreen(
                onClickCreateNewGroup = { viewModel.processIntent(GroupListIntent.ClickCreateNewGroup) },
                onClickEnterNewGroup = { viewModel.processIntent(GroupListIntent.ClickEnterNewGroup) },
                onDismissed = { viewModel.processIntent(GroupListIntent.DismissedTopBarChip) },
                modifier = modifier.padding(innerPadding),
            )
        }
    }
}
