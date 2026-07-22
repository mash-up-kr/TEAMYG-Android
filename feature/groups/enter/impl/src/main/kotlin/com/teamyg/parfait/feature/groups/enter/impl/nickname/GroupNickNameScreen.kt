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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.text.toStringResource

private const val NICKNAME_MAX_LENGTH = 15

@Composable
internal fun GroupNickNameScreen(
    uiState: GroupNickNameUiState,
    onValueChanged: (word: String) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier) {
        YGTopBarDetail(
            title = "그룹 참여하기",
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
                    text = "그룹이름에서 사용할\n닉네임을 입력해 주세요",
                    color = YGAtomicColors.Gray.Gray900,
                    style = YGTheme.typography.title.t02B,
                )
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))
                Text(
                    text = "그룹이름에서만 공유되는 닉네임이에요",
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
                    placeholder = "그룹에서 사용할 닉네임을 입력해 주세요",
                    isError = uiState.nickNameError != null,
                    maxLength = NICKNAME_MAX_LENGTH,
                    errorDescription = uiState.nickNameError?.toStringResource(),
                    colors = YGTextFormFieldDefaults.colors(),
                )
            }
        }

        YGButton(
            text = "확인",
            buttonType = YGButtonType.Large,
            isEnabled = uiState.nickName.isNotEmpty(),
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
            GroupNickNameUiState(""),
            GroupNickNameUiState(nickName = "he"),
            GroupNickNameUiState(nickName = "hello"),
        )
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
        modifier = Modifier.fillMaxSize(),
    )
}
