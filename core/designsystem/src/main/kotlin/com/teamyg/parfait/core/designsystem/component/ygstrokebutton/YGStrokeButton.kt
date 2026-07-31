package com.teamyg.parfait.core.designsystem.component.ygstrokebutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Figma Button-Stroke
 */
@Composable
fun YGStrokeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.none
    val isHighlighted = isEnabled && (isSelected || isPressed)
    val backgroundColor = if (isHighlighted) {
        YGAtomicColors.Gray.Gray100
    } else {
        YGAtomicColors.Gray.White
    }
    val borderColor = if (isEnabled) {
        YGAtomicColors.Gray.Gray500
    } else {
        YGAtomicColors.Gray.Gray200
    }
    val contentColor = if (isEnabled) {
        YGAtomicColors.Gray.Gray700
    } else {
        YGAtomicColors.Gray.Gray300
    }

    Row(
        modifier = modifier
            .height(SizeTokens.Size44.getDp())
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
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = YGTheme.layout.gap.gap1,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        iconResource?.let { resource ->
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = contentColor),
                modifier = Modifier.size(SizeTokens.Size20.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGStrokeButtonPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
        )
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            isSelected = true,
        )
        YGStrokeButton(
            text = "토핑 추가",
            onClick = {},
            iconResource = R.drawable.ic_plus,
            isEnabled = false,
        )
        YGStrokeButton(
            text = "아이콘 없음",
            onClick = {},
        )
    }
}
