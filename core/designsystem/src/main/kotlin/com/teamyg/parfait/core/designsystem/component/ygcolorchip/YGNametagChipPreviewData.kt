package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider

data class YGChipPreviewData(
    val name: String,
    val colorChipType: YGColorChipType,
)

class YGNametagChipPreviewParameterProvider : NamedPreviewParameterProvider<YGChipPreviewData>() {
    override val entries = listOf(
        "nametagChip1" to YGChipPreviewData(
            name = "NametagChip1",
            colorChipType = YGColorChipType.NametagChip1,
        ),
        "nametagChip2" to YGChipPreviewData(
            name = "NametagChip2",
            colorChipType = YGColorChipType.NametagChip2,
        ),
        "nametagChip3" to YGChipPreviewData(
            name = "NametagChip3",
            colorChipType = YGColorChipType.NametagChip3,
        ),
        "nametagChip4" to YGChipPreviewData(
            name = "NametagChip4",
            colorChipType = YGColorChipType.NametagChip4,
        ),
        "nametagChip5" to YGChipPreviewData(
            name = "NametagChip5",
            colorChipType = YGColorChipType.NametagChip5,
        ),
        "nametagChip6" to YGChipPreviewData(
            name = "NametagChip6",
            colorChipType = YGColorChipType.NametagChip6,
        ),
        "nametagChip7" to YGChipPreviewData(
            name = "NametagChip7",
            colorChipType = YGColorChipType.NametagChip7,
        ),
        "nametagChip8" to YGChipPreviewData(
            name = "NametagChip8",
            colorChipType = YGColorChipType.NametagChip8,
        ),
        "nametagChip9" to YGChipPreviewData(
            name = "NametagChip9",
            colorChipType = YGColorChipType.NametagChip9,
        ),
        "nametagChip10" to YGChipPreviewData(
            name = "NametagChip10",
            colorChipType = YGColorChipType.NametagChip10,
        ),
        "nametagChip11" to YGChipPreviewData(
            name = "NametagChip11",
            colorChipType = YGColorChipType.NametagChip11,
        ),
        "nametagChip12" to YGChipPreviewData(
            name = "NametagChip12",
            colorChipType = YGColorChipType.NametagChip12,
        ),
        "nametagChipPlus" to YGChipPreviewData(
            name = "NametagChipPlus",
            colorChipType = YGColorChipType.NametagChipPlus,
        ),
        "default" to YGChipPreviewData(
            name = "Default",
            colorChipType = YGColorChipType.Default,
        ),
    )
}
