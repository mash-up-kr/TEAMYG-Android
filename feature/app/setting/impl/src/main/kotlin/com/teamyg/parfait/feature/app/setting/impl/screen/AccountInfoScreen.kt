package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoUiState
import com.teamyg.parfait.core.ui.R as CoreR

@Composable
internal fun AccountInfoScreen(
    state: AccountInfoUiState,
    onValueChanged: (nickname: String) -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
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
                            top = YGTheme.layout.padding.padding8,
                            start = YGTheme.layout.padding.padding7,
                            end = YGTheme.layout.padding.padding7,
                        ),
                    ),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    YGLabel(text = stringResource(R.string.account_info_nickname_label))
                    YGTextFormField(
                        value = state.nickname,
                        onValueChange = onValueChanged,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.errorMessageResId != null,
                        maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
                        errorDescription = state.errorMessageResId?.let { stringResource(it) },
                        colors = YGTextFormFieldDefaults.colors(),
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
                errorMessageResId = CoreR.string.error_empty_space_nickname,
            ),
            AccountInfoUiState(
                nickname = " 가",
                errorMessageResId = CoreR.string.error_space_at_edge_nickname,
            ),
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
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
