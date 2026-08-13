package com.teamyg.parfait.feature.intro.impl.termagree

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import com.teamyg.parfait.domain.model.policy.PolicyVO
import com.teamyg.parfait.feature.intro.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun TermAgreeScreen(
    state: TermAgreeState,
    onClickTermAgree: (termsId: TermsId, newSelected: Boolean) -> Unit,
    onClickTermLandingUrl: (landingUrl: String) -> Unit,
    onClickAgreeAllTerm: (newSelected: Boolean) -> Unit,
    onClickNextButton: () -> Unit,
    onClickBackButton: () -> Unit,
    onClickRetryLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        YGTopBarBack(
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
                    text = stringResource(R.string.title_term_agree),
                    style = YGTheme.typography.title.t01B,
                    color = YGAtomicColors.Gray.Gray900,
                )
            }

            item {
                Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap9))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = YGAtomicColors.Gray.Gray100, shape = YGTheme.shapes.radius.small)
                        .clip(shape = YGTheme.shapes.radius.small)
                        .padding(YGTheme.layout.padding.padding3)
                        .clickable { onClickAgreeAllTerm(state.isAllSelected.not()) },
                ) {
                    Image(
                        painter = painterResource(DesignSystemR.drawable.ic_check_round),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(
                            color = if (state.isAllSelected) {
                                YGAtomicColors.Gray.Black
                            } else {
                                YGAtomicColors.Gray.Gray200
                            },
                        ),
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = stringResource(R.string.agree_all),
                        style = YGTheme.typography.body.b01B,
                        color = YGAtomicColors.Gray.Gray900,
                    )
                }
            }

            if (state.isLoadFailed) {
                item {
                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.term_agree_load_failed),
                            style = YGTheme.typography.body.b02R,
                            color = YGAtomicColors.Gray.Gray500,
                        )
                        Text(
                            text = stringResource(R.string.term_agree_retry),
                            style = YGTheme.typography.body.b02SB,
                            color = YGAtomicColors.Gray.Gray800,
                            modifier = Modifier.clickable { onClickRetryLoad() },
                        )
                    }
                }
            }

            items(state.policies, key = { policy -> policy.termsId.value }) { policy ->
                val isSelected = state.isAgreed(policy)
                if (policy == state.policies.first()) {
                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = YGTheme.layout.padding.padding6,
                            vertical = YGTheme.layout.padding.padding3,
                        ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClickTermAgree(policy.termsId, isSelected.not()) },
                    ) {
                        Image(
                            painter = painterResource(DesignSystemR.drawable.ic_check),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.Black),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))
                        if (policy.required) {
                            Text(
                                text = stringResource(R.string.prefix_required),
                                color = if (isSelected) YGAtomicColors.Gray.Gray800 else YGAtomicColors.Gray.Gray500,
                                style = YGTheme.typography.body.b02R,
                            )
                            Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))
                        }
                        Text(
                            text = policy.title,
                            color = if (isSelected) YGAtomicColors.Gray.Gray800 else YGAtomicColors.Gray.Gray500,
                            style = YGTheme.typography.body.b02R,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.width(YGTheme.layout.gap.gap2))
                    Image(
                        painter = painterResource(DesignSystemR.drawable.ic_caret_right),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.Gray500),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onClickTermLandingUrl(policy.url) },
                    )
                }
            }
        }

        YGButton(
            text = stringResource(R.string.submit),
            buttonType = YGButtonType.Large,
            isEnabled = state.isAvailable,
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

private class TermAgreeScreenPreviewParameterProvider : PreviewParameterProvider<TermAgreeState> {
    private val policies = listOf(
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
            title = "개인정보 처리방침",
            url = "https://example.com/privacy",
            required = true,
        ),
    )

    override val values: Sequence<TermAgreeState>
        get() = sequenceOf(
            TermAgreeState(policies = policies, isLoading = false),
            TermAgreeState(
                policies = policies,
                agreedTermsIds = setOf(TermsId(1L)),
                isLoading = false,
            ),
            TermAgreeState(
                policies = policies,
                agreedTermsIds = policies.map(PolicyVO::termsId).toSet(),
                isLoading = false,
            ),
            TermAgreeState(isLoading = false, isLoadFailed = true),
        )
}

@YGPreview
@Composable
private fun TermAgreeScreenPreview(
    @PreviewParameter(TermAgreeScreenPreviewParameterProvider::class) state: TermAgreeState,
) = PreviewBox {
    TermAgreeScreen(
        state = state,
        onClickTermAgree = { _, _ -> },
        onClickTermLandingUrl = {},
        onClickAgreeAllTerm = {},
        onClickNextButton = {},
        onClickBackButton = {},
        onClickRetryLoad = {},
        modifier = Modifier.fillMaxSize(),
    )
}
