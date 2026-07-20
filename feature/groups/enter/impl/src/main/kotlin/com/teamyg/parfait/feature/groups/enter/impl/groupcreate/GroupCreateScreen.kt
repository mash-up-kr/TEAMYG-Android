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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
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

private const val GROUP_NAME_MAX_LENGTH = 10
private const val NICKNAME_MAX_LENGTH = 10
private const val GROUP_ROW_COUNT = 6
private val GROUP_COUNT_LIST = (1..12).toList()

@Composable
internal fun GroupCreateScreen(
    uiState: GroupCreateUiState,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarDetail(
            title = "그룹 만들기",
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
                    text = "그룹명",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                YGTextFormField(
                    value = uiState.groupName,
                    onValueChange = {}, // Todo : 구현
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "그룹명을 입력해 주세요",
                    enabled = true,
                    isError = false, // Todo : 구현
                    maxLength = GROUP_NAME_MAX_LENGTH,
                    errorDescription = "", // Todo : 구현
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))
                YGLabel(
                    text = "그룹 속 내 닉네임",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                YGTextFormField(
                    value = uiState.nickName,
                    onValueChange = {}, // no-op
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "그룹명을 입력해 주세요",
                    enabled = true,
                    maxLength = NICKNAME_MAX_LENGTH,
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))

                YGLabel(
                    text = "그룹 인원",
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                VerticalGridLayout(
                    items = GROUP_COUNT_LIST,
                    rowCount = GROUP_ROW_COUNT,
                    verticalPadding = 6.dp,
                    horizontalPadding = 7.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) { number ->
                    YGInputNumber(
                        number = number,
                        isSelected = uiState.groupNumber == number,
                        onClick = { }, // Todo : 구현
                    )
                }

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))

                Text(
                    text = "그룹명과 인원수는 추후 변경할 수 없어요",
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray300,
                )
            }
        }

        YGButton(
            text = "확인",
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
        modifier = Modifier.fillMaxSize(),
    )
}
