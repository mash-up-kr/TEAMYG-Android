package com.teamyg.parfait.feature.camera.impl.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
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
    isEnabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White, shape = YGTheme.shapes.radius.round)
            .border(width = 1.dp, shape = CircleShape, color = YGAtomicColors.Transparency.Black5),
    ) {
        YGIconButton(
            iconResource = iconResource,
            size = size,
            contentDescription = contentDescription,
            onClick = onClick,
            isEnabled = isEnabled,
        )
    }
}
