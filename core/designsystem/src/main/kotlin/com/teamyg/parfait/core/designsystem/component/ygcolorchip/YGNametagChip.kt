package com.teamyg.parfait.core.designsystem.component.ygcolorchip

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

sealed interface YGNametagChipStyle {
    val colorChipSize: Dp
    val colorChipWidth: Dp
    val textStyle: TextStyle
        @Composable get

    data object Style28 : YGNametagChipStyle {
        override val colorChipSize = 28.dp
        override val colorChipWidth = 0.75.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.caption.c01R
    }

    data object Style40 : YGNametagChipStyle {
        override val colorChipSize = 40.dp
        override val colorChipWidth = 1.dp
        override val textStyle: TextStyle
            @Composable get() = YGTheme.typography.body.b01R
    }
}

@Composable
fun YGNametagChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGNametagChipStyle,
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

@YGPreview
@Composable
private fun YGNametagChipPreview(
    @PreviewParameter(YGNametagChipPreviewParameterProvider::class)
    data: YGChipPreviewData,
) = PreviewBox {
    Column {
        Text(data.name)
        Spacer(modifier = Modifier.height(5.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            YGNametagChip(
                colorChipType = data.colorChipType,
                userFirstName = "문",
                chip = YGNametagChipStyle.Style28,
            )
            YGNametagChip(
                colorChipType = data.colorChipType,
                userFirstName = "문",
                chip = YGNametagChipStyle.Style40,
            )
        }
    }
}
