package com.teamyg.parfait.core.designsystem.component.ygedittabbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * Figma Button-Edit-Tab
 */
@Composable
fun YGEditTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Column(
        modifier = modifier
            .selectable(
                selected = isSelected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ).width(IntrinsicSize.Max)
            .padding(
                horizontal = YGTheme.layout.padding.padding4,
                vertical = YGTheme.layout.padding.padding3,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = if (isSelected) YGTheme.typography.body.b01SB else YGTheme.typography.body.b01R,
            color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray500,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding2))
        if (isSelected) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.4.dp)
                    .background(color = YGAtomicColors.Gray.Gray900),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGEditTabButtonPreview() = PreviewBox {
    Row(horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2)) {
        YGEditTabButton(
            text = "토핑",
            isSelected = true,
            onClick = {},
        )
        YGEditTabButton(
            text = "사진",
            isSelected = false,
            onClick = {},
        )
    }
}
