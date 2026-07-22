package com.teamyg.parfait.core.designsystem.component.ygdatebutton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Composable
fun YGDateButton(
    text: String,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
            ).semantics { role = Role.Button }
            .padding(SizeTokens.Size6.getDp())
            .background(
                color = when {
                    isEnabled.not() -> YGAtomicColors.Gray.Transparent
                    isSelected -> YGAtomicColors.Gray.Gray900
                    isToday -> YGAtomicColors.Gray.Transparent
                    else -> YGAtomicColors.Gray.Transparent
                },
                shape = YGTheme.shapes.radius.round,
            ).border(
                width = 1.dp,
                color = when {
                    isEnabled.not() -> YGAtomicColors.Gray.Transparent
                    isSelected -> YGAtomicColors.Gray.Transparent
                    isToday -> YGAtomicColors.Gray.Gray850
                    else -> YGAtomicColors.Gray.Transparent
                },
                shape = YGTheme.shapes.radius.round,
            ),
    ) {
        Text(
            text = text,
            color = when {
                isEnabled.not() -> YGAtomicColors.Gray.Gray400
                isSelected -> YGAtomicColors.Gray.White
                isToday -> YGAtomicColors.Gray.Gray950
                else -> YGAtomicColors.Gray.Gray800
            },
            style = if (isSelected) YGTheme.typography.body.b02SB else YGTheme.typography.body.b02R,
            textAlign = TextAlign.Center,
        )
    }
}

@YGPreview
@Composable
private fun YGDateButtonPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White),
    ) {
        YGDateButton(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = true,
            onClick = {},
            modifier = Modifier.size(44.dp),
        )
        YGDateButton(
            text = "31",
            isSelected = true,
            isToday = false,
            isEnabled = true,
            onClick = {},
            modifier = Modifier.size(44.dp),
        )
        YGDateButton(
            text = "31",
            isSelected = false,
            isToday = true,
            isEnabled = true,
            onClick = {},
            modifier = Modifier.size(44.dp),
        )
        YGDateButton(
            text = "31",
            isSelected = false,
            isToday = false,
            isEnabled = false,
            onClick = {},
            modifier = Modifier.size(44.dp),
        )
    }
}
