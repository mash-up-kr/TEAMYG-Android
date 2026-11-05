package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider

data class YGChipButtonPreviewData(
    val startIconResource: Int?,
    val endIconResource: Int?,
    val colors: YGChipButtonColors,
)

class YGChipButtonPreviewParameterProvider : NamedPreviewParameterProvider<YGChipButtonPreviewData>() {
    override val entries = listOf(
        "cherrySolid" to YGChipButtonPreviewData(
            startIconResource = null,
            endIconResource = null,
            colors = YGChipButtonColorsDefaults.CherrySolid,
        ),
        "grayOutlineStartIcon" to YGChipButtonPreviewData(
            startIconResource = R.drawable.ic_plus,
            endIconResource = null,
            colors = YGChipButtonColorsDefaults.GrayOutline,
        ),
        "cherrySolidEndIcon" to YGChipButtonPreviewData(
            startIconResource = null,
            endIconResource = R.drawable.ic_plus,
            colors = YGChipButtonColorsDefaults.CherrySolid,
        ),
    )
}
