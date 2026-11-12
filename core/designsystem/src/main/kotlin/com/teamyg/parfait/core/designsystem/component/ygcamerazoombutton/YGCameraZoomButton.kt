package com.teamyg.parfait.core.designsystem.component.ygcamerazoombutton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

private val BORDER_WIDTH = 1.5.dp

/**
 * Figma Button-Camera-Zoom
 */
@Composable
fun YGCameraZoomButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round

    Box(
        modifier = modifier
            .size(SizeTokens.Size44.getDp())
            .clickableYGNoRipple(
                onClick = onClick,
                interactionSource = interactionSource,
            ).semantics { role = Role.Button }
            .padding(YGTheme.layout.padding.padding2),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    color = if (isPressed) {
                        YGAtomicColors.Transparency.Black75
                    } else {
                        YGAtomicColors.Transparency.Black50
                    },
                    shape = shape,
                ).border(
                    width = BORDER_WIDTH,
                    color = YGAtomicColors.Transparency.White25,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = YGTheme.typography.caption.c01M,
                color = YGAtomicColors.Gray.White,
            )
        }
    }
}

@YGPreview
@Composable
private fun YGCameraZoomButtonPreview() = PreviewBox {
    Row(
        modifier = Modifier
            .background(YGAtomicColors.Gray.Black)
            .padding(YGTheme.layout.padding.padding6),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        listOf("1x", "2x", "0.5x").forEach { label ->
            YGCameraZoomButton(label = label, onClick = {})
        }
    }
}
