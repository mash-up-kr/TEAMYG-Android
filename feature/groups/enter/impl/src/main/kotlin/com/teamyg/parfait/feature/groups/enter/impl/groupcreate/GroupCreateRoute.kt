package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupCreateRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupCreateSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is GroupCreateSideEffect.NavigateToNext -> {
                    // 여기까지 쌓인 화면(그룹명·인원수, 닉네임)은 전부 이번 생성 흐름의 것이라
                    // 되돌아갈 곳이 없다 — 백스택을 비우고 새 그룹의 캔버스만 남긴다
                    navigator.replaceAll(
                        destination = NavKeyCanvasMain(
                            groupId = effect.groupId,
                            welcomeGroupName = effect.groupName,
                            welcomeInviteCode = effect.inviteCode,
                        ),
                    )
                }
            }
        }
    }

    GroupCreateScreen(
        uiState = uiState,
        onClickNextButton = { viewModel.processIntent(GroupCreateIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupCreateIntent.ClickBackButton) },
        onGroupNameChanged = { viewModel.processIntent(GroupCreateIntent.InputGroupName(it)) },
        onClickGroupNumber = { viewModel.processIntent(GroupCreateIntent.ClickGroupNumber(it)) },
        onClickConfirmPopupCreate = { viewModel.processIntent(GroupCreateIntent.ClickConfirmPopupCreate) },
        onDismissConfirmPopup = { viewModel.processIntent(GroupCreateIntent.DismissConfirmPopup) },
        modifier = modifier,
    )
}
