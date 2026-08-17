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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
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

    /**
     * ViewModel 은 NavEntry 가 백스택에 남아 있는 한 살아 있어, 초기화 한 번으로는 돌아왔을 때
     * 낡은 목록이 그대로 남는다. 특히 그룹 생성·참여는 `goToSingleClearTop` 으로 이 엔트리를
     * 재사용하므로 방금 만든 그룹이 보이지 않았다.
     *
     * 백스택 아래에 깔린 엔트리는 컴포지션에서 빠지므로 이 효과는 다시 앞에 설 때 한 번 돈다.
     * 화면이 컴포지션에 있는 동안의 앱 복귀(ON_RESUME)도 같이 잡힌다.
     */
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
                    navigator.goTo(NavKeyGroupCreate(nickName = uiState.nickName))
                }

                is GroupListSideEffect.NavigateToInviteCode -> {
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
                onClickTopping = { groupId -> viewModel.processIntent(GroupListIntent.ClickTopping(groupId)) },
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
