package com.teamyg.parfait.core.designsystem.component.ygchipbutton

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme

@Composable
fun YGChipButton(
    text: String,
    colors: YGChipButtonColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes startIconResource: Int? = null,
    @DrawableRes endIconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = colors.backgroundColor(isPressed),
                shape = YGTheme.shapes.radius.round,
            ).clip(shape = YGTheme.shapes.radius.round)
            .border(
                width = 1.dp,
                color = colors.borderColor(isPressed),
                shape = YGTheme.shapes.radius.round,
            ).clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
            .padding(
                top = YGTheme.layout.padding.padding3,
                end = if (endIconResource != null) YGTheme.layout.padding.padding3 else YGTheme.layout.padding.padding5,
                bottom = YGTheme.layout.padding.padding3,
                start = if (startIconResource !=
                    null
                ) {
                    YGTheme.layout.padding.padding3
                } else {
                    YGTheme.layout.padding.padding5
                },
            ),
    ) {
        startIconResource?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = colors.foregroundColor(isPressed),
                ),
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            color = colors.foregroundColor(isPressed),
            style = YGTheme.typography.body.b02R,
        )
        endIconResource?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = colors.foregroundColor(isPressed),
                ),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun YGChipButtonPreview(
    @PreviewParameter(YGChipButtonPreviewParameterProvider::class)
    data: YGChipButtonPreviewData,
) {
    YGCustomTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            YGChipButton(
                text = "Parfait",
                onClick = {},
                startIconResource = data.startIconResource,
                colors = data.colors,
                endIconResource = data.endIconResource,
            )
        }
    }
}
