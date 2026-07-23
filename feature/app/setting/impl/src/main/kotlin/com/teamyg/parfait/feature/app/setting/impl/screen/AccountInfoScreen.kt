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
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.NicknameResult
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoUiState

private const val NICKNAME_MAX_LENGTH = 15

@Composable
internal fun AccountInfoScreen(
    state: AccountInfoUiState,
    onValueChanged: (nickname: String) -> Unit,
    onClickBack: () -> Unit,
    onClickLogout: () -> Unit,
    onClickWithdraw: () -> Unit,
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
                    // TODO Design System Label 변환
                    Text(
                        text = stringResource(R.string.account_info_nickname_label),
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray400,
                    )
                    YGTextFormField(
                        value = state.nickname,
                        onValueChange = onValueChanged,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.nicknameError != null,
                        maxLength = NICKNAME_MAX_LENGTH,
                        errorDescription = state.nicknameError?.toStringResource(),
                        colors = YGTextFormFieldDefaults.colors(),
                    )
                }

                YGDangerZone(
                    topZone = {
                        YGActionItem(
                            text = stringResource(R.string.account_info_logout),
                            onClick = onClickLogout,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    bottomZone = {
                        YGActionItem(
                            text = stringResource(R.string.account_info_withdraw),
                            onClick = onClickWithdraw,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                nicknameError = NicknameResult.Error.Empty,
            ),
            AccountInfoUiState(
                nickname = " 가",
                nicknameError = NicknameResult.Error.SpaceAtEdge,
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
        onClickLogout = {},
        onClickWithdraw = {},
        modifier = Modifier.fillMaxSize(),
    )
}
