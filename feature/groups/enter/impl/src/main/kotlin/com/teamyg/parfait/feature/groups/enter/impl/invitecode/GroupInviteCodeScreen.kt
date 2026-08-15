package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputField
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputFieldElement
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodePasteBar
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GroupInviteCodeScreen(
    uiState: GroupInviteCodeUiState,
    onValueChanged: (index: Int, word: String) -> Unit,
    onClickTextFieldElement: (index: Int) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    onClickConfirmPopupEnter: () -> Unit,
    onDismissConfirmPopup: () -> Unit,
    onClickPasteBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isConfirmPopupVisible) {
        YGModalPopup(
            title = stringResource(R.string.group_enter_confirm_title, uiState.groupName),
            body = stringResource(R.string.group_enter_confirm_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.confirm_popup_cancel),
            onSecondaryClick = onDismissConfirmPopup,
            primaryText = stringResource(R.string.group_enter_confirm_enter),
            onPrimaryClick = onClickConfirmPopupEnter,
            onDismissRequest = onDismissConfirmPopup,
        )
    }

    Column(modifier = modifier) {
        YGTopBarDetail(
            title = stringResource(R.string.group_enter),
            onIconClick = onClickBackButton,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = YGTheme.layout.padding.padding7,
                vertical = YGTheme.layout.padding.padding10,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item {
                Text(
                    text = stringResource(R.string.group_invite_title),
                    color = YGAtomicColors.Gray.Gray900,
                    style = YGTheme.typography.title.t02B,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
                Text(
                    text = stringResource(R.string.group_invite_description),
                    color = YGAtomicColors.Gray.Gray500,
                    style = YGTheme.typography.body.b02R,
                )
            }
            item {
                Spacer(modifier = Modifier.height(69.dp))
                InviteCodeInputField(
                    text = uiState.text,
                    maxLength = uiState.codeLength,
                    horizontalSpace = YGTheme.layout.gap.gap3,
                    modifier = Modifier.fillMaxWidth(),
                    elementContent = { word, index ->
                        InviteCodeInputFieldElement(
                            word = word,
                            isFocus = index == uiState.focusedIndex,
                            isError = uiState.inviteCodeError != null,
                            onValueChanged = { changed -> onValueChanged(index, changed) },
                            onClickTextFieldElement = { onClickTextFieldElement(index) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(7 / 8f),
                        )
                    },
                )
            }
            if (uiState.inviteCodeError != null) {
                item {
                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                    Text(
                        text = uiState.inviteCodeError.toStringResource(),
                        color = YGAtomicColors.Cherry.Cherry600,
                        style = YGTheme.typography.caption.c01R,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap12))
            }
        }

        YGButton(
            text = stringResource(R.string.submit),
            buttonType = YGButtonType.Large,
            isEnabled = uiState.text.length == uiState.codeLength && uiState.isSubmitting.not(),
            onClick = onClickNextButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = YGTheme.layout.padding.padding7),
        )

        val pasteBarInviteCode = uiState.pasteBarInviteCode
        if (pasteBarInviteCode != null) {
            InviteCodePasteBar(
                inviteCode = pasteBarInviteCode,
                onClick = onClickPasteBar,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private class GroupInviteCodeScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupInviteCodeUiState> {
    override val values: Sequence<GroupInviteCodeUiState>
        get() = sequenceOf(
            GroupInviteCodeUiState(""),
            GroupInviteCodeUiState(text = "he"),
            GroupInviteCodeUiState(text = "hello"),
            GroupInviteCodeUiState(text = "", inviteCodeError = InviteCodeError.MEMBER_LIMIT_REACHED),
            GroupInviteCodeUiState(
                text = "hello!",
                groupName = "모카의 파르페",
                isConfirmPopupVisible = true,
            ),
            GroupInviteCodeUiState(
                text = "",
                focusedIndex = 0,
                clipboardInviteCode = "E54W1A",
            ),
        )
}

@YGPreview
@Composable
private fun GroupInviteCodeScreenPreview(
    @PreviewParameter(GroupInviteCodeScreenPreviewParameterProvider::class) uiState: GroupInviteCodeUiState,
) = PreviewBox {
    GroupInviteCodeScreen(
        uiState = uiState,
        onValueChanged = { _, _ -> },
        onClickTextFieldElement = {},
        onClickNextButton = {},
        onClickBackButton = {},
        onClickConfirmPopupEnter = {},
        onDismissConfirmPopup = {},
        onClickPasteBar = {},
        modifier = Modifier.fillMaxSize(),
    )
}
