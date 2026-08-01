package com.teamyg.parfait.core.designsystem.component.ygcirclebutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Button-Circle
 */
@Composable
fun YGCircleButton(
    @DrawableRes iconResource: Int,
    type: YGCircleButtonType,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round
    val background = if (isPressed) type.pressedBackgroundColor else type.backgroundColor
    val fill = Modifier
        .background(
            color = background,
            shape = shape,
        ).border(
            width = 1.dp,
            color = type.borderColor,
            shape = shape,
        )

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
            .then(if (type.paintsOuterCircle) fill else Modifier)
            .padding(YGTheme.layout.padding.padding3),
        contentAlignment = Alignment.Center,
    ) {
        if (type.paintsOuterCircle) {
            YGCircleButtonIcon(
                iconResource = iconResource,
                contentDescription = contentDescription,
                iconTint = type.iconTint,
                iconSize = type.iconSize,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(SizeTokens.Size28.getDp())
                    .then(fill),
                contentAlignment = Alignment.Center,
            ) {
                YGCircleButtonIcon(
                    iconResource = iconResource,
                    contentDescription = contentDescription,
                    iconTint = type.iconTint,
                    iconSize = type.iconSize,
                )
            }
        }
    }
}

@Composable
private fun YGCircleButtonIcon(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    iconTint: Color,
    iconSize: Dp,
) {
    Image(
        painter = painterResource(id = iconResource),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color = iconTint),
        modifier = Modifier.size(iconSize),
    )
}

@YGPreview
@Composable
private fun YGCircleButtonPreview() = PreviewBox {
    Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4)) {
        YGCircleButton(
            iconResource = R.drawable.ic_caret_left,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = {},
        )
        YGCircleButton(
            iconResource = R.drawable.ic_plus,
            type = YGCircleButtonType.Secondary,
            contentDescription = null,
            onClick = {},
        )
        YGCircleButton(
            iconResource = R.drawable.ic_rotate,
            type = YGCircleButtonType.Small,
            contentDescription = null,
            onClick = {},
        )
    }
}
