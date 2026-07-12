package com.teamyg.parfait.core.designsystem.component.ygbuttonicon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGIconButton(
    @DrawableRes iconResource: Int,
    size: YGButtonIconSize,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isEnabled: Boolean = true,
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.containerSize)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
            ),
    ) {
        Image(
            painter = painterResource(iconResource),
            colorFilter = ColorFilter.tint(
                color = when {
                    isEnabled.not() -> YGAtomicColors.Gray.Gray200
                    isPressed -> YGAtomicColors.Gray.Gray400
                    else -> YGAtomicColors.Gray.Gray300
                },
            ),
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }
}

@Preview
@Composable
private fun YGIconButtonPreview(
    @PreviewParameter(YGButtonIconPreviewParameterProvider::class)
    data: YGButtonIconPreviewData,
) {
    YGCustomTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            YGIconButton(
                iconResource = R.drawable.ic_close_round,
                size = data.buttonIconSize,
                onClick = {},
                contentDescription = null,
                isEnabled = data.isEnabled,
            )
        }
    }
}
