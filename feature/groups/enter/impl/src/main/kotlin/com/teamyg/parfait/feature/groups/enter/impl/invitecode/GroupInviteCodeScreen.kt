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
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputField
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.component.InviteCodeInputFieldElement

@Composable
internal fun GroupInviteCodeScreen(
    uiState: GroupInviteCodeUiState,
    onValueChanged: (index: Int, word: String) -> Unit,
    onClickTextFieldElement: (index: Int) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                            isError = uiState.errorText != null,
                            onValueChanged = { changed -> onValueChanged(index, changed) },
                            onClickTextFieldElement = { onClickTextFieldElement(index) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(7 / 8f),
                        )
                    },
                )
            }
            if (uiState.errorText != null) {
                item {
                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                    Text(
                        text = uiState.errorText,
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
            isEnabled = uiState.text.length == uiState.codeLength,
            onClick = onClickNextButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = YGTheme.layout.padding.padding7),
        )
    }
}

private class GroupInviteCodeScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupInviteCodeUiState> {
    override val values: Sequence<GroupInviteCodeUiState>
        get() = sequenceOf(
            GroupInviteCodeUiState(""),
            GroupInviteCodeUiState(text = "he"),
            GroupInviteCodeUiState(text = "hello"),
            GroupInviteCodeUiState(text = "", errorText = "이미 최대 인원이 모두 참여한 그룹이에요"),
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
        modifier = Modifier.fillMaxSize(),
    )
}
