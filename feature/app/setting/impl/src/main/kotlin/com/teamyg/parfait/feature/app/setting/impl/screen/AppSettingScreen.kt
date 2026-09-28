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
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.ProfileCard
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun AppSettingScreen(
    state: AppSettingState,
    onClickBack: () -> Unit,
    onClickAccount: () -> Unit,
    onClickPolicy: (TermsId) -> Unit,
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
                    loginProviderText = state.loginProvider?.toStringResource(),
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
                    state.policies.forEach { policy ->
                        PolicyListItem(
                            policy = policy,
                            onClick = { onClickPolicy(policy.termsId) },
                        )
                    }
                    YGListItem(
                        text = stringResource(R.string.setting_item_version),
                        subText = stringResource(R.string.setting_item_version_value, state.version),
                    )
                }

                YGDangerZone(
                    topZone = {
                        YGActionItem(
                            text = stringResource(R.string.setting_logout),
                            onClick = onClickLogout,
                            // 요청이 나가 있는 동안 비활성 — 연타가 ViewModel 에서 삼켜지는
                            // 것과 별개로, 눌리는 것처럼 보이지 않아야 한다
                            enabled = !state.isLoggingOut,
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

/**
 * 약관 목록 한 줄. title·url 이 둘 다 비어 있으면 줄을 그리지 않는다.
 * title 만 비어 있으면 서버가 일부러 빈 문자열을 내려준 것으로 보고 빈 텍스트로 그대로 보여준다.
 */
@Composable
private fun PolicyListItem(
    policy: PolicyVO,
    onClick: () -> Unit,
) {
    if (policy.title.isNotBlank() || policy.url.isNotBlank()) {
        YGListItem(
            text = policy.title,
            trailingIcon = DesignSystemR.drawable.ic_caret_right,
            onClickTrailingIcon = onClick,
        )
    }
}

@YGPreview
@Composable
private fun AppSettingScreenPreview() = PreviewBox {
    AppSettingScreen(
        state = AppSettingState(
            nickname = "아니야나그런데기니야",
            loginProvider = LoginProvider.KAKAO,
            policies = listOf(
                PolicyVO(
                    termsId = TermsId(1L),
                    type = PolicyType.TERMS_OF_SERVICE,
                    title = "서비스 이용약관",
                    url = "https://example.com/terms",
                    required = true,
                ),
                PolicyVO(
                    termsId = TermsId(2L),
                    type = PolicyType.PRIVACY_POLICY,
                    title = "개인정보처리방침",
                    url = "https://example.com/privacy",
                    required = true,
                ),
            ),
        ),
        onClickBack = {},
        onClickAccount = {},
        onClickPolicy = {},
        onClickLogout = {},
        onClickWithdraw = {},
        onConfirmWithdraw = {},
        onDismissWithdrawDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
