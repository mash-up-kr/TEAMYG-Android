package com.teamyg.parfait.core.designsystem.component.modal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGModalPopup(
    title: String,
    body: String,
    @DrawableRes iconRes: Int,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabledButton: Boolean = true,
    iconTint: Color = YGAtomicColors.Cherry.Cherry600,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        YGModalPopupContent(
            title = title,
            body = body,
            iconRes = iconRes,
            secondaryText = secondaryText,
            onSecondaryClick = onSecondaryClick,
            primaryText = primaryText,
            onPrimaryClick = onPrimaryClick,
            modifier = modifier,
            isEnabledButton = isEnabledButton,
            iconTint = iconTint,
        )
    }
}

@Composable
private fun YGModalPopupContent(
    title: String,
    body: String,
    @DrawableRes iconRes: Int,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabledButton: Boolean = true,
    iconTint: Color = YGAtomicColors.Cherry.Cherry600,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5),
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = YGAtomicColors.Gray.White,
                shape = YGTheme.shapes.radius.medium1,
            ).clip(shape = YGTheme.shapes.radius.medium1)
            .padding(
                start = YGTheme.layout.padding.padding6,
                top = YGTheme.layout.padding.padding5,
                end = YGTheme.layout.padding.padding6,
                bottom = YGTheme.layout.padding.padding6,
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.size(SizeTokens.Size48.getDp()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconTint),
                    modifier = Modifier.size(SizeTokens.Size32.getDp()),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = YGTheme.typography.title.t03SB,
                    color = YGAtomicColors.Gray.Gray900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = body,
                    style = YGTheme.typography.body.b02R,
                    color = YGAtomicColors.Gray.Gray500,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
            modifier = Modifier.fillMaxWidth(),
        ) {
            YGButton(
                text = secondaryText,
                buttonType = YGButtonType.Medium.Secondary,
                isEnabled = isEnabledButton,
                onClick = onSecondaryClick,
                modifier = Modifier.weight(1f),
            )

            YGButton(
                text = primaryText,
                buttonType = YGButtonType.Medium.Primary,
                isEnabled = isEnabledButton,
                onClick = onPrimaryClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGModalPopup() = PreviewBox {
    YGModalPopup(
        title = "그룹에서 나갈까요?",
        body = "그룹에서 나가도\n그룹에 올렸던 사진은 지워지지 않아요",
        iconRes = R.drawable.ic_warning_round,
        secondaryText = "나가기",
        onSecondaryClick = {},
        primaryText = "취소",
        onPrimaryClick = {},
        onDismissRequest = {},
    )
}
