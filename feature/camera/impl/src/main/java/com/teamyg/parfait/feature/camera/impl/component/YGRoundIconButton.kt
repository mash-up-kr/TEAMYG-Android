package com.teamyg.parfait.feature.camera.impl.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
internal fun YGRoundIconButton(
    @DrawableRes iconResource: Int,
    size: YGIconButtonSize,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White, shape = YGTheme.shapes.radius.round)
            .border(width = 1.dp, shape = CircleShape, color = YGAtomicColors.Transparency.Black5)
            .size(size.containerSize)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
            ),
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }
}
