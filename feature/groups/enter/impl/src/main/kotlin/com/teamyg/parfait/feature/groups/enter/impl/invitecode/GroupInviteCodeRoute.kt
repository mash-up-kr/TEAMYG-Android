package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import android.content.ClipDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.isSensitive
import com.teamyg.parfait.core.util.android.extension.navigationBarsAndImePadding
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupNickName
import com.teamyg.parfait.core.ui.R as CoreUiR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupInviteCodeRoute(
    navigator: Navigator,
    navKey: NavKeyGroupInviteCode,
    modifier: Modifier = Modifier,
    viewModel: GroupInviteCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val imeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboard.current
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    val inviteMessageTemplate = stringResource(CoreUiR.string.group_invite_message)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GroupInviteCodeSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is GroupInviteCodeSideEffect.NavigateToNext -> {
                    navigator.goTo(
                        NavKeyGroupNickName(
                            inviteCode = effect.inviteCode,
                            groupName = effect.groupName,
                            nickName = effect.nickName,
                        ),
                    )
                }
            }
        }
    }

    // Android 10 부터 포커스를 가진 앱만 클립보드를 읽을 수 있어 윈도우 포커스 기준으로 확인한다.
    // 다른 앱에서 초대코드를 복사하고 돌아온 경우도 이 시점에 다시 감지된다.
    LaunchedEffect(isWindowFocused) {
        if (isWindowFocused) {
            val inviteCode = clipboard.readInviteCodeOrNull(inviteMessageTemplate)
            viewModel.processIntent(GroupInviteCodeIntent.ClipboardCodeDetected(inviteCode?.value))
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible.not()) {
            viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard)
        }
    }

    // App Links 로 들어왔으면(navKey.inviteCode) 입력칸만 미리 채운다 — 참여 여부는 사용자가
    // 화면을 보고 직접 다음 버튼을 눌러 확정한다. 수동 진입이면 첫 칸에 포커스를 준다.
    LaunchedEffect(Unit) {
        val prefillCode = navKey.inviteCode
        if (prefillCode != null) {
            viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = prefillCode, cursor = prefillCode.length))
        } else {
            viewModel.processIntent(GroupInviteCodeIntent.RequestFocus)
        }
    }

    LaunchedEffect(uiState.isFocused) {
        if (uiState.isFocused) {
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        isLoading = uiState.isSubmitting,
    ) { innerPadding ->
        GroupInviteCodeScreen(
            uiState = uiState,
            onTextChanged = { text, cursor ->
                viewModel.processIntent(GroupInviteCodeIntent.ChangeText(text = text, cursor = cursor))
            },
            onClickTextFieldElement = { index ->
                viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index))
            },
            onClickNextButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton) },
            onClickBackButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickBackButton) },
            onClickPasteBar = { viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode) },
            onFocusChanged = { isFocused ->
                viewModel.processIntent(GroupInviteCodeIntent.FocusChanged(isFocused = isFocused))
            },
            onClickBackground = { viewModel.processIntent(GroupInviteCodeIntent.HideKeyboard) },
            modifier = Modifier
                .fillMaxSize()
                .background(YGAtomicColors.Gray.White)
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsAndImePadding(),
        )
    }
}

/**
 * 클립보드에 초대코드로 볼 수 있는 텍스트가 있으면 반환한다.
 *
 * 실제 텍스트를 읽기 전에 [ClipDescription] 으로 먼저 걸러낸다.
 * description 조회는 Android 12 부터 뜨는 붙여넣기 안내 토스트를 유발하지 않는다.
 */
private suspend fun Clipboard.readInviteCodeOrNull(messageTemplate: String): InviteCode? {
    val description = nativeClipboard.primaryClipDescription ?: return null
    if (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN).not()) {
        return null
    }
    if (description.isSensitive()) {
        return null
    }

    val clipData = getClipEntry()?.clipData ?: return null
    if (clipData.itemCount == 0) {
        return null
    }

    return InviteCode.parseOrNull(
        text = clipData.getItemAt(0).text?.toString(),
        messageTemplate = messageTemplate,
    )
}
