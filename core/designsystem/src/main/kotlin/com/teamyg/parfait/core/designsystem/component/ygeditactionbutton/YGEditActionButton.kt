package com.teamyg.parfait.core.designsystem.component.ygeditactionbutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * Figma Button-Edit-Action
 */
@Composable
fun YGEditActionButton(
    @DrawableRes iconResource: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()
    val shape = YGTheme.shapes.radius.round
    val background = when {
        isEnabled.not() -> YGAtomicColors.Transparency.Black5
        isPressed -> YGAtomicColors.Transparency.Black75
        else -> YGAtomicColors.Transparency.Black50
    }

    Box(
        modifier = modifier
            .clickableYGNoRipple(
                enabled = isEnabled,
                onClick = onClick,
                interactionSource = interactionSource,
            ).semantics { role = Role.Button }
            .padding(YGTheme.layout.padding.padding1),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = background,
                    shape = shape,
                ).border(
                    width = 1.5.dp,
                    color = YGAtomicColors.Transparency.White25,
                    shape = shape,
                ).padding(YGTheme.layout.padding.padding3),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = iconResource),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(color = YGAtomicColors.Gray.White),
                modifier = Modifier.size(SizeTokens.Size24.getDp()),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGEditActionButtonPreview() = PreviewBox {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = Modifier
            .background(YGAtomicColors.Gray.Gray900)
            .padding(YGTheme.layout.padding.padding6),
    ) {
        YGEditActionButton(
            iconResource = R.drawable.ic_arrow_left,
            contentDescription = null,
            onClick = {},
        )
        YGEditActionButton(
            iconResource = R.drawable.ic_arrow_left,
            contentDescription = null,
            onClick = {},
            isEnabled = false,
        )
    }
}
