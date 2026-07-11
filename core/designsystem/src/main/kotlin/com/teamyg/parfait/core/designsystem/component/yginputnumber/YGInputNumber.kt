package com.teamyg.parfait.core.designsystem.component.yginputnumber

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGInputNumber(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGInputNumber(
        number = number,
        isSelected = isSelected,
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
    )
}

@Composable
private fun YGInputNumber(
    number: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(50.dp) // 디자인가이드상 50x50 고정
            .background(
                color = when {
                    isSelected -> YGAtomicColors.Gray.Gray900
                    else -> YGAtomicColors.Gray.White
                },
                shape = YGTheme.shapes.radius.xSmall,
            ).border(
                width = 1.dp,
                color = if (isSelected) YGAtomicColors.Gray.Gray900 else YGAtomicColors.Gray.Gray100,
                shape = YGTheme.shapes.radius.xSmall,
            ).clip(
                shape = YGTheme.shapes.radius.xSmall,
            ),
    ) {
        Text(
            text = number.toString(),
            style = YGTheme.typography.body.b01R,
            color = if (isSelected) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Gray900,
        )
    }
}

@Preview
@Composable
private fun YGInputNumberPreview(
    @PreviewParameter(YGInputNumberPreviewParameterProvider::class)
    data: YGInputNumberPreviewData,
) {
    YGCustomTheme {
        Box(modifier = Modifier.fillMaxWidth()) {
            YGInputNumber(
                number = 3,
                isSelected = data.isSelected,
            )
        }
    }
}
