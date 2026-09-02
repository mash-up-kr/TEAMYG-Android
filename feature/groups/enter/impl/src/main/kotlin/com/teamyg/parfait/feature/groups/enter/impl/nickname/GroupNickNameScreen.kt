package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.text.NameFieldType
import com.teamyg.parfait.core.util.android.focus.clearFocusOnTap
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GroupNickNameScreen(
    uiState: GroupNickNameUiState,
    onValueChanged: (word: String) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    onClickConfirmPopupEnter: () -> Unit,
    onDismissConfirmPopup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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

    Column(modifier = modifier.clearFocusOnTap()) {
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
                    text = stringResource(R.string.group_nickname_title),
                    color = YGAtomicColors.Gray.Gray900,
                    style = YGTheme.typography.title.t02B,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
                Text(
                    text = stringResource(R.string.group_nickname_description),
                    color = YGAtomicColors.Gray.Gray500,
                    style = YGTheme.typography.body.b02R,
                )
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap10))

                YGTextFormField(
                    value = uiState.nickName,
                    onValueChange = { value -> onValueChanged(value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = stringResource(R.string.group_nickname_placeholder),
                    isError = uiState.nicknameError != null,
                    maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
                    errorDescription = uiState.nicknameError?.toStringResource(NameFieldType.NICKNAME),
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
        }

        YGButton(
            text = stringResource(R.string.submit),
            buttonType = YGButtonType.Large,
            isEnabled = uiState.nickName.isNotEmpty() && uiState.isEntering.not(),
            onClick = onClickNextButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = YGTheme.layout.padding.padding7),
        )
    }
}

private class GroupNickNameScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupNickNameUiState> {
    override val values: Sequence<GroupNickNameUiState>
        get() = sequenceOf(
            GroupNickNameUiState(groupName = GROUP_NAME),
            GroupNickNameUiState(groupName = GROUP_NAME, nickName = "he"),
            GroupNickNameUiState(groupName = GROUP_NAME, nickName = "hello"),
            GroupNickNameUiState(
                groupName = GROUP_NAME,
                nickName = " hello",
                nicknameError = NameValidResult.Error.SpaceAtEdge,
            ),
            GroupNickNameUiState(
                groupName = GROUP_NAME,
                nickName = "hello",
                isConfirmPopupVisible = true,
            ),
        )

    private companion object {
        const val GROUP_NAME = "모카의 파르페"
    }
}

@YGPreview
@Composable
private fun GroupNickNameScreenPreview(
    @PreviewParameter(GroupNickNameScreenPreviewParameterProvider::class) uiState: GroupNickNameUiState,
) = PreviewBox {
    GroupNickNameScreen(
        uiState = uiState,
        onValueChanged = { },
        onClickNextButton = {},
        onClickBackButton = {},
        onClickConfirmPopupEnter = {},
        onDismissConfirmPopup = {},
        modifier = Modifier.fillMaxSize(),
    )
}
