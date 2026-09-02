package com.teamyg.parfait.feature.groups.enter.impl.invitecode.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.enter.impl.R

/**
 * 클립보드에서 찾은 초대코드를 키보드 바로 위에 노출하는 바.
 *
 * 화면 최하단에 배치되며, 상위에서 ime inset 만큼 패딩이 적용되어 키보드에 붙는다.
 */
@Composable
internal fun InviteCodePasteBar(
    inviteCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
        modifier = modifier
            .fillMaxWidth()
            .background(YGAtomicColors.Gray.Gray200)
            .clickableYGNoRipple(onClick = onClick)
            .padding(
                vertical = YGTheme.layout.padding.padding4,
                horizontal = YGTheme.layout.padding.padding7,
            ),
    ) {
        Text(
            text = stringResource(R.string.invite_code_paste_bar_label),
            color = YGAtomicColors.Gray.Black,
            style = YGTheme.typography.body.b02R,
        )
        Text(
            text = inviteCode,
            color = YGAtomicColors.Gray.Black,
            style = YGTheme.typography.body.b01SB,
        )
    }
}

private class InviteCodePasteBarPreviewParameterProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() = sequenceOf(
            "E54W1A",
            "WDIDCJ",
        )
}

@YGPreview
@Composable
private fun InviteCodePasteBarPreview(
    @PreviewParameter(InviteCodePasteBarPreviewParameterProvider::class) inviteCode: String,
) = PreviewBox {
    InviteCodePasteBar(
        inviteCode = inviteCode,
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
