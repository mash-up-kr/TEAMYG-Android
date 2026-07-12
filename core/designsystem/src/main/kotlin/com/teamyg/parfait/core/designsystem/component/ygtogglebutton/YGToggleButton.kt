package com.teamyg.parfait.core.designsystem.component.ygtogglebutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = if (isSelected) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Transparent,
                shape = YGTheme.shapes.radius.round,
            ).clip(shape = YGTheme.shapes.radius.round)
            .clickable(onClick = onClick, indication = null, interactionSource = interactionSource)
            .semantics { role = Role.Button }
            .padding(
                top = YGTheme.layout.padding.padding3,
                end = YGTheme.layout.padding.padding5,
                bottom = YGTheme.layout.padding.padding3,
                start = if (iconResource != null) YGTheme.layout.padding.padding3 else YGTheme.layout.padding.padding5,
            ),
    ) {
        iconResource?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Transparency.Black50,
                ),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = text,
            color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Transparency.Black50,
            style = YGTheme.typography.body.b01SB,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun YGToggleButtonPreview(
    @PreviewParameter(YGToggleButtonPreviewParameterProvider::class)
    data: YGToggleButtonPreviewData,
) {
    YGCustomTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(YGAtomicColors.Cherry.Cherry50),
        ) {
            YGToggleButton(
                text = "Parfait",
                isSelected = data.isSelected,
                onClick = {},
                iconResource = data.iconResource,
            )
        }
    }
}
