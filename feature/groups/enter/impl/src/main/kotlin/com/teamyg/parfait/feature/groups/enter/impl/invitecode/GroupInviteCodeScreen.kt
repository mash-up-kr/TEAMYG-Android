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
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputField
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputFieldElement
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodePasteBar

@Composable
internal fun GroupInviteCodeScreen(
    uiState: GroupInviteCodeUiState,
    onTextChanged: (text: String, cursor: Int) -> Unit,
    onClickTextFieldElement: (index: Int) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    onClickPasteBar: () -> Unit,
    onFocusChanged: (isFocused: Boolean) -> Unit,
    onClickBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 상단바·입력칸·버튼은 각자 클릭을 먹으므로 그 바깥을 눌렀을 때만 걸린다
    Column(modifier = modifier.clickableYGNoRipple(onClick = onClickBackground)) {
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
                    cursor = uiState.cursor,
                    isFocused = uiState.isFocused,
                    maxLength = uiState.codeLength,
                    horizontalSpace = YGTheme.layout.gap.gap3,
                    onTextChanged = onTextChanged,
                    onFocusChanged = onFocusChanged,
                    onDone = onClickNextButton,
                    modifier = Modifier.fillMaxWidth(),
                    elementContent = { word, index ->
                        InviteCodeInputFieldElement(
                            word = word,
                            isFocus = uiState.isFocused && index == uiState.focusedIndex,
                            isError = uiState.inviteCodeError != null,
                            onClick = { onClickTextFieldElement(index) },
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
            GroupInviteCodeUiState(text = "he", focusedIndex = 2, isFocused = true),
            GroupInviteCodeUiState(text = "hello", focusedIndex = 5, isFocused = true),
            GroupInviteCodeUiState(text = "", inviteCodeError = InviteCodeError.MEMBER_LIMIT_REACHED),
            GroupInviteCodeUiState(
                text = "",
                focusedIndex = 0,
                isFocused = true,
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
        onTextChanged = { _, _ -> },
        onClickTextFieldElement = {},
        onClickNextButton = {},
        onClickBackButton = {},
        onClickPasteBar = {},
        onFocusChanged = {},
        onClickBackground = {},
        modifier = Modifier.fillMaxSize(),
    )
}
