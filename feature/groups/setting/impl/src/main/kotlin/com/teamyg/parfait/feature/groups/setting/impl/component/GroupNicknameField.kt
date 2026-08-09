package com.teamyg.parfait.feature.groups.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.text.NameFieldType
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.feature.groups.setting.impl.R

@Composable
internal fun GroupNicknameField(
    nickname: String,
    nicknameError: NameValidResult.Error?,
    onNicknameChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onConfirmNickname: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
    ) {
        YGLabel(text = stringResource(R.string.group_setting_nickname_label))

        YGTextFormField(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> onFocusChange(focusState.hasFocus) },
            isError = nicknameError != null,
            maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
            errorDescription = nicknameError?.toStringResource(NameFieldType.NICKNAME),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirmNickname() }),
        )
    }
}

private data class GroupNicknameFieldPreviewState(
    val nickname: String,
    val nicknameError: NameValidResult.Error? = null,
)

private class GroupNicknameFieldPreviewParameterProvider :
    PreviewParameterProvider<GroupNicknameFieldPreviewState> {
    override val values: Sequence<GroupNicknameFieldPreviewState>
        get() = sequenceOf(
            GroupNicknameFieldPreviewState(nickname = "닉네임"),
            GroupNicknameFieldPreviewState(nickname = ""),
            GroupNicknameFieldPreviewState(
                nickname = "닉네임!",
                nicknameError = NameValidResult.Error.InvalidCharacter,
            ),
            GroupNicknameFieldPreviewState(nickname = "열다섯글자를꽉꽉채운닉네임야호"),
        )
}

@YGPreview
@Composable
private fun GroupNicknameFieldPreview(
    @PreviewParameter(GroupNicknameFieldPreviewParameterProvider::class)
    state: GroupNicknameFieldPreviewState,
) = PreviewBox {
    GroupNicknameField(
        nickname = state.nickname,
        nicknameError = state.nicknameError,
        onNicknameChange = {},
        onFocusChange = {},
        onConfirmNickname = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(YGTheme.layout.padding.padding7),
    )
}
