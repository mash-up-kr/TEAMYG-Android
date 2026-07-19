package com.teamyg.parfait.core.designsystem.component.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGInviteCard(
    label: String,
    inviteCode: String,
    subText: String,
    status: YGInviteCardStatus,
    copyButtonText: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes endIconResource: Int? = R.drawable.ic_copy,
) {
    val borderColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Cherry.Cherry100
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray100
    }
    val subTextColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Gray.Gray600
        YGInviteCardStatus.Invalid -> YGAtomicColors.Cherry.Cherry600
    }
    Column(
        modifier = modifier
            .border(
                width = SizeTokens.Size1.getDp(),
                color = borderColor,
                shape = YGTheme.shapes.radius.none,
            ).clip(YGTheme.shapes.radius.medium1)
            .background(YGAtomicColors.Gray.White)
            .padding(
                horizontal = YGTheme.layout.padding.padding6,
                vertical = YGTheme.layout.padding.padding5,
            ),
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = YGTheme.typography.body.b02R,
                color = YGAtomicColors.Gray.Gray400,
            )

            Text(
                text = subText,
                style = YGTheme.typography.body.b02R,
                color = subTextColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InviteCodeBox(
                inviteCode = inviteCode,
                status = status,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            YGButton(
                text = copyButtonText,
                buttonType = YGButtonType.SmallSquare,
                isEnabled = status == YGInviteCardStatus.Active,
                onClick = onCopyClick,
                endIconResource = endIconResource,
            )
        }
    }
}

@Composable
private fun InviteCodeBox(
    inviteCode: String,
    status: YGInviteCardStatus,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Cherry.Cherry100
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray200
    }
    val codeColor = when (status) {
        YGInviteCardStatus.Active -> YGAtomicColors.Gray.Gray900
        YGInviteCardStatus.Invalid -> YGAtomicColors.Gray.Gray500
    }

    Box(
        modifier = modifier
            .clip(YGTheme.shapes.radius.none)
            .background(backgroundColor)
            .padding(
                horizontal = YGTheme.layout.padding.padding8,
                vertical = YGTheme.layout.padding.padding3,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = inviteCode,
            style = YGTheme.typography.body.b01SB,
            color = codeColor,
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGInviteCard() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        YGInviteCard(
            label = "그룹 초대 코드",
            inviteCode = "WDIDCJ",
            subText = "1명 남음",
            status = YGInviteCardStatus.Active,
            copyButtonText = "복사",
            onCopyClick = {},
        )
        YGInviteCard(
            label = "그룹 초대 코드",
            inviteCode = "WDIDCJ",
            subText = "최대 인원 도달",
            status = YGInviteCardStatus.Invalid,
            copyButtonText = "복사",
            onCopyClick = {},
        )
    }
}
