package com.teamyg.parfait.core.designsystem.component.ygchip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme

sealed interface YGColorChipStyle {
    val colorChipSize: Dp
    val colorChipWidth: Dp
    val textStyle: TextStyle
        @Composable get

    data object Style28 : YGColorChipStyle {
        override val colorChipSize = 28.dp
        override val colorChipWidth = 0.75.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.caption.c01R
    }

    data object Style40 : YGColorChipStyle {
        override val colorChipSize = 40.dp
        override val colorChipWidth = 1.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.body.b01R
    }
}

@Composable
fun YGColorChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGColorChipStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(chip.colorChipSize)
            .clip(CircleShape)
            .background(colorChipType.fillColor)
            .border(
                width = chip.colorChipWidth,
                color = colorChipType.strokeColor,
                shape = CircleShape,
            ),
    ) {
        Text(
            text = userFirstName,
            color = colorChipType.textColor,
            style = chip.textStyle,
        )
    }
}

@Preview
@Composable
private fun YGChipPreview(
    @PreviewParameter(YGColorChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) {
    YGCustomTheme {
        Column {
            Text(data.name)
            Spacer(modifier = Modifier.height(5.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                YGColorChip(
                    colorChipType = data.colorChipType,
                    userFirstName = "문",
                    chip = YGColorChipStyle.Style28,
                )
                YGColorChip(
                    colorChipType = data.colorChipType,
                    userFirstName = "문",
                    chip = YGColorChipStyle.Style40,
                )
            }
        }
    }
}
