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

    // `isLoading` 을 넘기지 않는다. 이 화면의 실패는 입력 자리 인라인(`submitError`,
    // 형식 오류와 같은 자리)이 정본이라 토스트가 아니고, 제출 중 표현도 확인 버튼 비활성
    // (`isConfirmEnabled`)이 이미 하고 있다. 그 위에 전면 Dim 을 얹는 것은 UX 변경이라
    // 디자인 결정이 필요하다 → ygscaffold-v2 스펙 "주의 / 열린 질문"
    YGScaffoldV2(modifier = modifier) { innerPadding ->
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
