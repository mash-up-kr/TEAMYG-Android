package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.toStringResource as globalNicknameErrorToStringResource

@Composable
internal fun AccountInfoScreen(
    state: AccountInfoUiState,
    onValueChanged: (nickname: String) -> Unit,
    onClickConfirm: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier.clearFocusOnTap()) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.account_info_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap8),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues = PaddingValues(
                            start = YGTheme.layout.padding.padding7,
                            end = YGTheme.layout.padding.padding7,
                        ),
                    ),
            ) {
                val nickname = state.nickname
                if (nickname == null) {
                    // SSoT 가 아직 값을 방출하지 않았다 — 빈 문자열이 아니라 로딩을 보여준다
                    Text(
                        text = stringResource(R.string.account_info_nickname_loading),
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray400,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        YGLabel(text = stringResource(R.string.account_info_nickname_label))
                        YGTextFormField(
                            value = nickname,
                            onValueChange = onValueChanged,
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.nicknameError != null || state.submitError != null,
                            maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
                            errorDescription = state.nicknameError?.toStringResource(NameFieldType.NICKNAME)
                                ?: state.submitError?.globalNicknameErrorToStringResource(),
                            colors = YGTextFormFieldDefaults.colors(),
                        )
                    }

                    YGButton(
                        text = stringResource(R.string.account_info_submit),
                        buttonType = YGButtonType.Large,
                        isEnabled = nickname.isNotBlank() && state.nicknameError == null && state.isSubmitting.not(),
                        onClick = onClickConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        OnBack { onClickBack() }
    }
}

private class AccountInfoPreviewParameterProvider :
    PreviewParameterProvider<AccountInfoUiState> {
    override val values: Sequence<AccountInfoUiState>
        get() = sequenceOf(
            AccountInfoUiState(nickname = "대충지은랜덤닉네임"),
            AccountInfoUiState(nickname = "닉네임바꾸"),
            AccountInfoUiState(
                nickname = "",
                nicknameError = NameValidResult.Error.EmptyString,
            ),
            AccountInfoUiState(
                nickname = " 가",
                nicknameError = NameValidResult.Error.SpaceAtEdge,
            ),
            AccountInfoUiState(
                nickname = "닉네임바꾸",
                submitError = GlobalNicknameError.INVALID,
            ),
            AccountInfoUiState(nickname = null),
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
        onClickConfirm = {},
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
