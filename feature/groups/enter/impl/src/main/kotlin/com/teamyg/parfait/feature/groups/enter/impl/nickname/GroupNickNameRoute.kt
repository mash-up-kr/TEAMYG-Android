package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain

@Composable
fun GroupNickNameRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupNickNameViewModel,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupNickNameSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is GroupNickNameSideEffect.NavigateToNext -> {
                    // 여기까지 쌓인 화면(초대코드, 닉네임)은 전부 이번 참여 흐름의 것이라
                    // 되돌아갈 곳이 없다 — 백스택을 비우고 참여한 그룹의 캔버스만 남긴다
                    navigator.replaceAll(
                        destination = NavKeyCanvasMain(
                            groupId = effect.groupId,
                            welcomeGroupName = effect.groupName,
                        ),
                    )
                }
            }
        }
    }

    GroupNickNameScreen(
        uiState = uiState,
        onValueChanged = { word -> viewModel.processIntent(GroupNickNameIntent.InputWord(word)) },
        onClickNextButton = { viewModel.processIntent(GroupNickNameIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupNickNameIntent.ClickBackButton) },
        onClickConfirmPopupEnter = { viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter) },
        onDismissConfirmPopup = { viewModel.processIntent(GroupNickNameIntent.DismissConfirmPopup) },
        modifier = modifier,
    )
}
