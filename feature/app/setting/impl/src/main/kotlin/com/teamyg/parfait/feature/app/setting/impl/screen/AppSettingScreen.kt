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
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
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
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
        }
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
        modifier = Modifier.fillMaxSize(),
    )
}
