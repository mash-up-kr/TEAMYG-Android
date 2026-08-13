package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import android.content.ClipDescription
import android.os.Build
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupNickName
import com.teamyg.parfait.core.ui.R as CoreUiR

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupInviteCodeRoute(
    navigator: Navigator,
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
                GroupInviteCodeSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                GroupInviteCodeSideEffect.NavigateToNext -> {
                    navigator.goTo(NavKeyGroupNickName)
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

    LaunchedEffect(Unit) {
        viewModel.processIntent(GroupInviteCodeIntent.FocusedFirstIndex)
    }

    LaunchedEffect(uiState.focusedIndex) {
        if (uiState.focusedIndex == null) {
            keyboardController?.hide()
        } else {
            keyboardController?.show()
        }
    }

    GroupInviteCodeScreen(
        uiState = uiState,
        onValueChanged = { index, word -> viewModel.processIntent(GroupInviteCodeIntent.InputWord(index, word)) },
        onClickTextFieldElement = { index ->
            viewModel.processIntent(GroupInviteCodeIntent.SelectedTextFieldElement(index))
        },
        onClickNextButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickNextButton) },
        onClickBackButton = { viewModel.processIntent(GroupInviteCodeIntent.ClickBackButton) },
        onClickConfirmPopupEnter = { viewModel.processIntent(GroupInviteCodeIntent.ClickConfirmPopupEnter) },
        onDismissConfirmPopup = { viewModel.processIntent(GroupInviteCodeIntent.DismissConfirmPopup) },
        onClickPasteBar = { viewModel.processIntent(GroupInviteCodeIntent.ClickPasteInviteCode) },
        modifier = modifier,
    )
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

private fun ClipDescription.isSensitive(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }

    return extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
}
