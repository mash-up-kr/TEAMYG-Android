package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGButtonIcon(
    @DrawableRes iconResource: Int,
    size: YGButtonIconSize,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    YGButtonIcon(
        iconResource = iconResource,
        size = size,
        isPressed = isPressed,
        contentDescription = contentDescription,
        modifier = modifier.clickable(onClick = onClick, interactionSource = interactionSource),
    )
}

@Composable
fun YGButtonIcon(
    @DrawableRes iconResource: Int,
    size: YGButtonIconSize,
    isPressed: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size.containerSize),
    ) {
        Image(
            painter = painterResource(iconResource),
            colorFilter = ColorFilter.tint(color = if (isPressed) YGAtomicColors.Gray.Gray400 else YGAtomicColors.Gray.Gray300),
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }
}

