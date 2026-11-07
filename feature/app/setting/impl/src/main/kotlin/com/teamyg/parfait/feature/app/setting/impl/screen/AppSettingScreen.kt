package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGListItem
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtextbutton.YGTextButton
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.ui.component.ListItemMenuSub
import com.teamyg.parfait.core.ui.text.toStringResource
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.ProfileCard
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AppSettingState
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/** 피그마 고정값 — 토큰 체계에 없는 화면 전용 여백이라 상수로 뺀다 */
private val FOOTER_LINK_HORIZONTAL_PADDING = 64.dp

@Composable
internal fun AppSettingScreen(
    state: AppSettingState,
    onClickBack: () -> Unit,
    onClickAccount: () -> Unit,
    onClickNotification: () -> Unit,
    onClickNotice: () -> Unit,
    onClickPolicy: (TermsId) -> Unit,
    onClickCsaePolicy: () -> Unit,
    onClickOssLicense: () -> Unit,
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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap5))

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
                        text = stringResource(R.string.setting_item_notification),
                        trailingIcon = DesignSystemR.drawable.ic_caret_right,
                        onClickTrailingIcon = onClickNotification,
                    )
                    ListItemMenuSub(
                        title = stringResource(R.string.setting_item_notice),
                        onClick = onClickNotice,
                        isNew = true,
                        modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding7),
                    )
                    YGListItem(
                        text = stringResource(R.string.setting_item_version),
                        subText = stringResource(R.string.setting_item_version_value, state.version),
                    )
                }

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

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

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap7))

                // 목록과 달리 서버가 값을 안 내려줘도 줄 자체는 그대로 보여준다 — 다만
                // 그 타입의 정책을 못 찾으면 눌러도 아무 일이 없다(termsId 가 없어 열 수 없다)
                val termsOfServicePolicy = state.policies.firstOrNull { it.type == PolicyType.TERMS_OF_SERVICE }
                val privacyPolicy = state.policies.firstOrNull { it.type == PolicyType.PRIVACY_POLICY }

                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = FOOTER_LINK_HORIZONTAL_PADDING,
                            end = FOOTER_LINK_HORIZONTAL_PADDING,
                            top = YGTheme.layout.padding.padding6,
                        ),
                ) {
                    PolicyLinkRow(
                        firstText = stringResource(R.string.setting_item_terms_of_service),
                        secondText = stringResource(R.string.setting_item_privacy_policy),
                        onClickFirst = { termsOfServicePolicy?.termsId?.let(onClickPolicy) },
                        onClickSecond = { privacyPolicy?.termsId?.let(onClickPolicy) },
                    )
                    PolicyLinkRow(
                        firstText = stringResource(R.string.setting_item_oss_license),
                        secondText = stringResource(R.string.setting_item_csae_policy),
                        onClickFirst = onClickOssLicense,
                        onClickSecond = onClickCsaePolicy,
                    )
                }

                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap9))
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

/** 약관 footer 한 줄 — 텍스트 버튼 둘을 구분선으로 가른다 */
@Composable
private fun PolicyLinkRow(
    firstText: String,
    secondText: String,
    onClickFirst: () -> Unit,
    onClickSecond: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YGTextButton(text = firstText, onClick = onClickFirst)
        Spacer(
            modifier = Modifier
                .width(SizeTokens.Size1.getDp())
                .height(YGTheme.layout.gap.gap3)
                .background(color = YGAtomicColors.Gray.Gray200),
        )
        YGTextButton(text = secondText, onClick = onClickSecond)
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
        onClickNotification = {},
        onClickNotice = {},
        onClickPolicy = {},
        onClickCsaePolicy = {},
        onClickOssLicense = {},
        onClickLogout = {},
        onClickWithdraw = {},
        onConfirmWithdraw = {},
        onDismissWithdrawDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
