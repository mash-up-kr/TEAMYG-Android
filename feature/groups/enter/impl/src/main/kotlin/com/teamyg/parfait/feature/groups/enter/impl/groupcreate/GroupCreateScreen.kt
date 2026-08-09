package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.yginputnumber.YGInputNumber
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.VerticalGridLayout
import com.teamyg.parfait.core.ui.text.NameFieldType
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun GroupCreateScreen(
    uiState: GroupCreateUiState,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    onGroupNameChanged: (newGroupName: String) -> Unit,
    onClickGroupNumber: (newSelectedNumber: Int) -> Unit,
    onClickConfirmPopupCreate: () -> Unit,
    onDismissConfirmPopup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isConfirmPopupVisible) {
        YGModalPopup(
            title = stringResource(R.string.group_create_confirm_title, uiState.groupName),
            body = stringResource(R.string.group_create_confirm_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.confirm_popup_cancel),
            onSecondaryClick = onDismissConfirmPopup,
            primaryText = stringResource(R.string.group_create_confirm_create),
            onPrimaryClick = onClickConfirmPopupCreate,
            onDismissRequest = onDismissConfirmPopup,
            isEnabledButton = uiState.isCreating.not(),
        )
    }

    Column(modifier = modifier) {
        YGTopBarDetail(
            title = stringResource(R.string.group_create),
            onIconClick = onClickBackButton,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = YGTheme.layout.padding.padding7,
                end = YGTheme.layout.padding.padding7,
                top = YGTheme.layout.padding.padding6,
                bottom = YGTheme.layout.padding.padding10,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            item {
                YGLabel(
                    text = stringResource(R.string.group_name_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                YGTextFormField(
                    value = uiState.groupName,
                    onValueChange = onGroupNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_name_placeholder),
                    enabled = true,
                    isError = uiState.groupNameError != null,
                    maxLength = GroupCreateConfig.GROUP_NAME_MAX_LENGTH,
                    errorDescription = uiState.groupNameError?.toStringResource(NameFieldType.GROUP_NAME),
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))
                YGLabel(
                    text = stringResource(R.string.group_nickname_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                YGTextFormField(
                    value = uiState.nickName,
                    onValueChange = {}, // no-op
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(R.string.group_nickname_placeholder),
                    enabled = false,
                    maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))

                YGLabel(
                    text = stringResource(R.string.group_count_label),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                VerticalGridLayout(
                    items = GroupCreateConfig.GROUP_COUNT_LIST,
                    columnCount = GroupCreateConfig.GROUP_COLUMN_COUNT,
                    verticalPadding = 6.dp,
                    horizontalPadding = 7.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) { selectedNumber ->
                    YGInputNumber(
                        number = selectedNumber,
                        isSelected = uiState.groupNumber == selectedNumber,
                        onClick = { onClickGroupNumber(selectedNumber) },
                    )
                }

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                Text(
                    text = stringResource(R.string.group_count_description),
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray300,
                )
            }
        }

        YGButton(
            text = stringResource(R.string.submit),
            buttonType = YGButtonType.Large,
            isEnabled = uiState.isValid,
            onClick = onClickNextButton,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding1,
                ),
        )
    }
}

private class GroupCreateScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupCreateUiState> {
    override val values: Sequence<GroupCreateUiState>
        get() = sequenceOf(
            GroupCreateUiState(""),
            GroupCreateUiState("", "", null),
            GroupCreateUiState("가나", "하이하이", 3),
            GroupCreateUiState("가나", "하이하이", 3, isConfirmPopupVisible = true),
        )
}

@YGPreview
@Composable
private fun GroupCreateScreenPreview(
    @PreviewParameter(GroupCreateScreenPreviewParameterProvider::class) uiState: GroupCreateUiState,
) = PreviewBox {
    GroupCreateScreen(
        uiState = uiState,
        onClickNextButton = {},
        onClickBackButton = {},
        onGroupNameChanged = {},
        onClickGroupNumber = {},
        onClickConfirmPopupCreate = {},
        onDismissConfirmPopup = {},
        modifier = Modifier.fillMaxSize(),
    )
}
