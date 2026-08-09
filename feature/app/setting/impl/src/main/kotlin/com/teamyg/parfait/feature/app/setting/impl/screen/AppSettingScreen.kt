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
import com.teamyg.parfait.core.designsystem.component.etc.YGListItem
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.ProfileCard
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun AppSettingScreen(
    state: AppSettingState,
    onClickBack: () -> Unit,
    onClickAccount: () -> Unit,
    onClickTerms: () -> Unit,
    onClickPrivacy: () -> Unit,
    onClickLogout: () -> Unit,
    onClickWithdraw: () -> Unit,
    onConfirmWithdraw: () -> Unit,
    onDismissWithdrawDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarBack(
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
                        ),
                    ),
            ) {
                ProfileCard(
                    nickname = state.nickname,
                    loginProvider = state.loginProvider,
                    modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding7),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    YGListItem(
                        text = stringResource(R.string.setting_item_account),
                        trailingIcon = DesignSystemR.drawable.ic_caret_right,
                        onClickTrailingIcon = onClickAccount,
                    )
                    YGListItem(
                        text = stringResource(R.string.setting_item_service_terms),
                        trailingIcon = DesignSystemR.drawable.ic_caret_right,
                        onClickTrailingIcon = onClickTerms,
                    )
                    YGListItem(
                        text = stringResource(R.string.setting_item_privacy_policy),
                        trailingIcon = DesignSystemR.drawable.ic_caret_right,
                        onClickTrailingIcon = onClickPrivacy,
                    )
                    YGListItem(
                        text = stringResource(R.string.setting_item_version),
                        subText = state.version,
                    )
                }

                YGDangerZone(
                    topZone = {
                        YGActionItem(
                            text = stringResource(R.string.setting_logout),
                            onClick = onClickLogout,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    bottomZone = {
                        YGActionItem(
                            text = stringResource(R.string.setting_withdraw),
                            onClick = onClickWithdraw,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = YGTheme.layout.padding.padding7),
                )
            }
        }

        if (state.isWithdrawDialogVisible) {
            YGModalPopup(
                title = stringResource(R.string.setting_withdraw_dialog_title),
                body = stringResource(R.string.setting_withdraw_dialog_body),
                iconRes = DesignSystemR.drawable.ic_warning_round,
                secondaryText = stringResource(R.string.setting_withdraw_dialog_confirm),
                onSecondaryClick = onConfirmWithdraw,
                primaryText = stringResource(R.string.setting_dialog_cancel),
                onPrimaryClick = onDismissWithdrawDialog,
                onDismissRequest = onDismissWithdrawDialog,
            )
        }

        OnBack { onClickBack() }
    }
}

@YGPreview
@Composable
private fun AppSettingScreenPreview() = PreviewBox {
    AppSettingScreen(
        state = AppSettingState(),
        onClickBack = {},
        onClickAccount = {},
        onClickTerms = {},
        onClickPrivacy = {},
        onClickLogout = {},
        onClickWithdraw = {},
        onConfirmWithdraw = {},
        onDismissWithdrawDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
