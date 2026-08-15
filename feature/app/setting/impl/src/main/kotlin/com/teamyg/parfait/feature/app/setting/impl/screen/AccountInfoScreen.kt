package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.text.NameFieldType
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.core.util.android.focus.clearFocusOnTap
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoUiState
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.GlobalNicknameError
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.toStringResource as globalNicknameErrorToStringResource

@Composable
internal fun AccountInfoScreen(
    state: AccountInfoUiState,
    onValueChanged: (nickname: String) -> Unit,
    onFocusChanged: (hasFocus: Boolean) -> Unit,
    onClickConfirm: () -> Unit,
    onClickBack: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val confirmAndDismissKeyboard = {
        onClickConfirm()
        focusManager.clearFocus()
    }
    val handleBack = {
        // 되돌릴 것이 없는데 키보드만 올라와 있으면 그것부터 내린다 — 뒤로가기 한 번이
        // 화면을 통째로 나가버리지 않게. 되돌릴 것이 있으면 판단은 ViewModel 이 한다.
        if (state.isEditing && state.isDirty.not()) focusManager.clearFocus() else onClickBack()
    }
    val keyboardOptions = remember { KeyboardOptions(imeAction = ImeAction.Done) }
    val keyboardActions = remember(confirmAndDismissKeyboard) {
        KeyboardActions(onDone = { confirmAndDismissKeyboard() })
    }

    YGScreen(modifier = modifier.clearFocusOnTap()) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.account_info_title),
                onIconClick = handleBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                    ),
            ) {
                YGLabel(text = stringResource(R.string.account_info_nickname_label))
                YGTextFormField(
                    // SSoT 가 아직 값을 내놓지 않았으면 빈 채로 비활성한다 — 레이아웃은
                    // 그대로 두고 만질 수 없게만 해서 값이 도착할 때 화면이 튀지 않는다.
                    value = state.nickname.orEmpty(),
                    onValueChange = onValueChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState -> onFocusChanged(focusState.hasFocus) },
                    enabled = state.savedNickname != null,
                    isError = state.nicknameError != null || state.submitError != null,
                    maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
                    errorDescription = state.nicknameError?.toStringResource(NameFieldType.NICKNAME)
                        ?: state.submitError?.globalNicknameErrorToStringResource(),
                    colors = YGTextFormFieldDefaults.colors(),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                )
            }

            if (state.isEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .background(YGAtomicColors.Gray.White)
                        .pointerInput(Unit) { detectTapGestures { /* 버튼 스트립에서 소비 */ } }
                        .padding(YGTheme.layout.padding.padding7),
                ) {
                    YGButton(
                        text = stringResource(R.string.account_info_submit),
                        buttonType = YGButtonType.Large,
                        isEnabled = state.isConfirmEnabled,
                        onClick = confirmAndDismissKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state.isDiscardDialogVisible) {
            YGModalPopup(
                title = stringResource(R.string.account_info_discard_dialog_title),
                body = stringResource(R.string.account_info_discard_dialog_body),
                iconRes = DesignSystemR.drawable.ic_warning_round,
                secondaryText = stringResource(R.string.account_info_discard_dialog_cancel),
                onSecondaryClick = onDismissDiscardDialog,
                primaryText = stringResource(R.string.account_info_discard_dialog_confirm),
                onPrimaryClick = onConfirmDiscard,
                onDismissRequest = onDismissDiscardDialog,
            )
        }

        OnBack { handleBack() }
    }
}

private class AccountInfoPreviewParameterProvider :
    PreviewParameterProvider<AccountInfoUiState> {
    override val values: Sequence<AccountInfoUiState>
        get() = sequenceOf(
            AccountInfoUiState(savedNickname = "대충지은랜덤닉네임", nickname = "대충지은랜덤닉네임"),
            AccountInfoUiState(
                savedNickname = "대충지은랜덤닉네임",
                nickname = "닉네임바꾸",
                isEditing = true,
            ),
            AccountInfoUiState(
                savedNickname = "대충지은랜덤닉네임",
                nickname = "",
                nicknameError = NameValidResult.Error.EmptyString,
                isEditing = true,
            ),
            AccountInfoUiState(
                savedNickname = "대충지은랜덤닉네임",
                nickname = " 가",
                nicknameError = NameValidResult.Error.SpaceAtEdge,
                isEditing = true,
            ),
            AccountInfoUiState(
                savedNickname = "대충지은랜덤닉네임",
                nickname = "닉네임바꾸",
                submitError = GlobalNicknameError.INVALID,
                isEditing = true,
            ),
            AccountInfoUiState(
                savedNickname = "대충지은랜덤닉네임",
                nickname = "닉네임바꾸",
                isEditing = true,
                isDiscardDialogVisible = true,
            ),
            AccountInfoUiState(savedNickname = null, nickname = null),
        )
}

@YGPreview
@Composable
private fun AccountInfoScreenPreview(
    @PreviewParameter(AccountInfoPreviewParameterProvider::class) uiState: AccountInfoUiState,
) = PreviewBox {
    AccountInfoScreen(
        state = uiState,
        onValueChanged = {},
        onFocusChanged = {},
        onClickConfirm = {},
        onClickBack = {},
        onConfirmDiscard = {},
        onDismissDiscardDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
