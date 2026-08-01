package com.teamyg.parfait.core.designsystem.component.ygeditbutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Button-Edit
 */
@Composable
fun YGEditButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val shape = YGTheme.shapes.radius.none
    val backgroundColor = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.White
    val borderColor = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray100
    val contentColor = if (isSelected) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Gray900

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = shape,
            ).clip(shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            ).selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ).padding(vertical = YGTheme.layout.padding.padding3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02SB,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGEditButtonPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGEditButton(
            text = "편집",
            isSelected = false,
            onClick = {},
            iconResource = R.drawable.ic_minus_round,
        )
        YGEditButton(
            text = "편집",
            isSelected = true,
            onClick = {},
            iconResource = R.drawable.ic_minus_round,
        )
        YGEditButton(
            text = "아이콘 없음",
            isSelected = false,
            onClick = {},
        )
    }
}
