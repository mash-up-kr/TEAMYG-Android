package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.AccountInfoScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoViewModel

@Composable
internal fun AccountInfoRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AccountInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountInfoSideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    // 닉네임 변경은 네트워크 왕복이라 언제 끝날지 보장이 없다. 확인 버튼 비활성
    // (`isConfirmEnabled`)은 "지금 눌러도 소용없다"만 말할 뿐 대기 자체를 표현하지 못하고,
    // 그동안 입력 필드는 살아 있어 PATCH 가 나간 뒤에도 값을 더 고칠 수 있다.
    //
    // 실패는 이 오버레이가 다루지 않는다 — 입력 자리 인라인(`submitError`, 형식 오류와 같은
    // 자리)이 정본이라 토스트가 아니다. `toastPolicy` 를 안 넘기는 이유다.
    YGScaffoldV2(
        modifier = modifier,
        isLoading = state.isSubmitting,
    ) { innerPadding ->
        AccountInfoScreen(
            state = state,
            onValueChanged = { viewModel.processIntent(AccountInfoIntent.InputWord(it)) },
            onFocusChanged = { viewModel.processIntent(AccountInfoIntent.ChangeFocus(it)) },
            onClickConfirm = { viewModel.processIntent(AccountInfoIntent.ClickConfirm) },
            onClickBack = { viewModel.processIntent(AccountInfoIntent.ClickBack) },
            onConfirmDiscard = { viewModel.processIntent(AccountInfoIntent.ConfirmDiscard) },
            onDismissDiscardDialog = { viewModel.processIntent(AccountInfoIntent.DismissDiscardDialog) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
