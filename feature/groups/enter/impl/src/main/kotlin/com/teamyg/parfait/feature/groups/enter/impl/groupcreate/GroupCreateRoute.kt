package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.navigationBarsAndImePadding
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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupCreateSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupCreateSideEffect.NavigateToNext -> {
                    navigator.goToSingleClearTop(destination = NavKeyGroupList)
                }

                is GroupCreateSideEffect.ShowError -> {
                    toastPolicy.showError(errorMessages.getValue(effect.error))
                }
            }
        }
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
