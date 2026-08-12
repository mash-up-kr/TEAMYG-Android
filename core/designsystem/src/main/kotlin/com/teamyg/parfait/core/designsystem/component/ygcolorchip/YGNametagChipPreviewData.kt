package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

data class YGChipPreviewData(
    val name: String,
    val colorChipType: YGColorChipType,
)

class YGNametagChipPreviewParameterProvider : PreviewParameterProvider<YGChipPreviewData> {
    override val values = sequenceOf(
        YGChipPreviewData(
            name = "NametagChip1",
            colorChipType = YGColorChipType.NametagChip1,
        ),
        YGChipPreviewData(
            name = "NametagChip2",
            colorChipType = YGColorChipType.NametagChip2,
        ),
        YGChipPreviewData(
            name = "NametagChip3",
            colorChipType = YGColorChipType.NametagChip3,
        ),
        YGChipPreviewData(
            name = "NametagChip4",
            colorChipType = YGColorChipType.NametagChip4,
        ),
        YGChipPreviewData(
            name = "NametagChip5",
            colorChipType = YGColorChipType.NametagChip5,
        ),
        YGChipPreviewData(
            name = "NametagChip6",
            colorChipType = YGColorChipType.NametagChip6,
        ),
        YGChipPreviewData(
            name = "NametagChip7",
            colorChipType = YGColorChipType.NametagChip7,
        ),
        YGChipPreviewData(
            name = "NametagChip8",
            colorChipType = YGColorChipType.NametagChip8,
        ),
        YGChipPreviewData(
            name = "NametagChip9",
            colorChipType = YGColorChipType.NametagChip9,
        ),
        YGChipPreviewData(
            name = "NametagChip10",
            colorChipType = YGColorChipType.NametagChip10,
        ),
        YGChipPreviewData(
            name = "NametagChip11",
            colorChipType = YGColorChipType.NametagChip11,
        ),
        YGChipPreviewData(
            name = "NametagChip12",
            colorChipType = YGColorChipType.NametagChip12,
        ),
        YGChipPreviewData(
            name = "NametagChipPlus",
            colorChipType = YGColorChipType.NametagChipPlus,
        ),
    )
}
