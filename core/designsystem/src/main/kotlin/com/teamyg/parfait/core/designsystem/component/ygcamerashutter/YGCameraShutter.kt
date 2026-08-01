package com.teamyg.parfait.core.designsystem.component.ygcamerashutter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Camera-Shutter
 */
@Composable
fun YGCameraShutter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round

    Box(
        modifier = modifier
            .background(
                color = YGAtomicColors.Gray.White,
                shape = shape,
            ).clip(shape)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
            .padding(YGTheme.layout.padding.padding2),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(SizeTokens.Size48.getDp())
                .background(
                    color = if (isPressed) YGAtomicColors.Gray.Gray950 else YGAtomicColors.Gray.Gray900,
                    shape = shape,
                ),
        )
    }
}

@YGPreview
@Composable
private fun YGCameraShutterPreview() = PreviewBox {
    Box(
        modifier = Modifier
            .background(YGAtomicColors.Gray.Black)
            .padding(YGTheme.layout.padding.padding6),
    ) {
        YGCameraShutter(onClick = {})
    }
}
