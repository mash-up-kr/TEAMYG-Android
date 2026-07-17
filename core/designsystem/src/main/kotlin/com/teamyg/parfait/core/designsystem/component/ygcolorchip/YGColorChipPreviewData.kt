package com.teamyg.parfait.core.designsystem.component.ygchip

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType

data class YGChipPreviewData(
    val name: String,
    val colorChipType: YGColorChipType,
)

class YGColorChipPreviewParameterProvider : PreviewParameterProvider<YGChipPreviewData> {
    override val values = sequenceOf(
        YGChipPreviewData(
            name = "nametagChip1",
            colorChipType = YGColorChipType.nametagChip1,
        ),
        YGChipPreviewData(
            name = "nametagChip2",
            colorChipType = YGColorChipType.nametagChip2,
        ),
        YGChipPreviewData(
            name = "nametagChip3",
            colorChipType = YGColorChipType.nametagChip3,
        ),
        YGChipPreviewData(
            name = "nametagChip4",
            colorChipType = YGColorChipType.nametagChip4,
        ),
        YGChipPreviewData(
            name = "nametagChip5",
            colorChipType = YGColorChipType.nametagChip5,
        ),
        YGChipPreviewData(
            name = "nametagChip6",
            colorChipType = YGColorChipType.nametagChip6,
        ),
        YGChipPreviewData(
            name = "nametagChip7",
            colorChipType = YGColorChipType.nametagChip7,
        ),
        YGChipPreviewData(
            name = "nametagChip8",
            colorChipType = YGColorChipType.nametagChip8,
        ),
        YGChipPreviewData(
            name = "nametagChip9",
            colorChipType = YGColorChipType.nametagChip9,
        ),
        YGChipPreviewData(
            name = "nametagChip10",
            colorChipType = YGColorChipType.nametagChip10,
        ),
        YGChipPreviewData(
            name = "nametagChip11",
            colorChipType = YGColorChipType.nametagChip11,
        ),
        YGChipPreviewData(
            name = "nametagChip12",
            colorChipType = YGColorChipType.nametagChip12,
        ),
        YGChipPreviewData(
            name = "nametagChip13",
            colorChipType = YGColorChipType.nametagChip13,
        ),
        YGChipPreviewData(
            name = "nametagChipPlus",
            colorChipType = YGColorChipType.nametagChipPlus,
        ),
    )
}
