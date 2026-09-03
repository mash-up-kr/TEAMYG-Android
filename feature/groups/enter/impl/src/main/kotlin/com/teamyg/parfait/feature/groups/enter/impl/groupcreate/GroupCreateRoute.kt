package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.navigationBarsAndImePadding
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.enter.impl.component.NotificationPermissionGate
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList

@Composable
fun GroupCreateRoute(
    navigator: Navigator,
    viewModel: GroupCreateViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트는 코루틴에서 수집돼 그 안에서 `stringResource` 를 부를 수 없다 — 문구를 미리 뽑아 둔다
    val errorMessages = GroupCreateError.entries.associateWith { it.toStringResource() }

    // 그룹 생성 직후 알림 권한 안내(정책 §3.1)를 한 번 거쳐야 캔버스로 넘어간다 —
    // 안내가 끝나기 전까지는 목적지 정보만 들고 대기한다
    var pendingNavigation by remember { mutableStateOf<GroupCreateSideEffect.NavigateToNext?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupCreateSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is GroupCreateSideEffect.NavigateToNext -> {
                    pendingNavigation = effect
                }

                is GroupCreateSideEffect.ShowError -> {
                    toastPolicy.showError(errorMessages.getValue(effect.error))
                }
            }
        }
    }

    pendingNavigation?.let { effect ->
        NotificationPermissionGate(
            onFinished = {
                pendingNavigation = null
                // 여기까지 쌓인 화면(그룹명·인원수, 닉네임)은 전부 이번 생성 흐름의 것이라
                // 되돌아갈 곳이 없다 — 백스택을 그룹 목록까지 비운 뒤 새 그룹의 캔버스를
                // 쌓는다. 캔버스만 남기면 백버튼이 앱 종료로 새 나간다
                navigator.replaceAll(destination = NavKeyGroupList)
                navigator.goTo(
                    destination = NavKeyCanvasMain(
                        groupId = effect.groupId,
                        welcomeGroupName = effect.groupName,
                        welcomeInviteCode = effect.inviteCode,
                    ),
                )
            },
        )
    }

    YGScaffoldV2(
        modifier = modifier,
        containerColor = YGAtomicColors.Gray.White,
        contentWindowInsets = WindowInsets(0.dp),
        isLoading = uiState.isCreating,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        GroupCreateScreen(
            uiState = uiState,
            onClickNextButton = { viewModel.processIntent(GroupCreateIntent.ClickNextButton) },
            onClickBackButton = { viewModel.processIntent(GroupCreateIntent.ClickBackButton) },
            onGroupNameChanged = { viewModel.processIntent(GroupCreateIntent.InputGroupName(it)) },
            onNickNameChanged = { viewModel.processIntent(GroupCreateIntent.InputNickName(it)) },
            onClickGroupNumber = { viewModel.processIntent(GroupCreateIntent.ClickGroupNumber(it)) },
            onClickConfirmPopupCreate = { viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate) },
            onDismissConfirmPopup = { viewModel.processIntent(GroupCreateIntent.DismissConfirmPopup) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsAndImePadding(),
        )
    }
}
