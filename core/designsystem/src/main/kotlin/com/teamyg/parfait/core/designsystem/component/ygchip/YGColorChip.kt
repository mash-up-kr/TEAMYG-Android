package com.teamyg.parfait.core.designsystem.component.ygchip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme

sealed interface YGChipStyle {
    val circleSize: Dp
    val textStyle: TextStyle
        @Composable get

    data object Style28 : YGChipStyle {
        override val circleSize = 28.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.caption.c01R
    }

    data object Style40 : YGChipStyle {
        override val circleSize = 40.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.body.b01R
    }
}

@Composable
fun YGChip(
    fillColor: Color,
    strokeColor: Color,
    textColor: Color,
    text: String,
    chip: YGChipStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(chip.circleSize)
            .clip(CircleShape)
            .background(fillColor)
            .border(
                width = 0.75.dp,
                color = strokeColor,
                shape = CircleShape,
            ),
    ) {
        Text(
            text = text,
            color = textColor,
            style = chip.textStyle,
        )
    }
}

@Preview
@Composable
private fun YGChipPreview(
    @PreviewParameter(YGChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) {
    YGCustomTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YGChip(
                fillColor = data.fillColor,
                strokeColor = data.strokeColor,
                textColor = data.textColor,
                text = "문",
                chip = YGChipStyle.Style28,
            )
            YGChip(
                fillColor = data.fillColor,
                strokeColor = data.strokeColor,
                textColor = data.textColor,
                text = "문",
                chip = YGChipStyle.Style40,
            )
        }
    }
}
