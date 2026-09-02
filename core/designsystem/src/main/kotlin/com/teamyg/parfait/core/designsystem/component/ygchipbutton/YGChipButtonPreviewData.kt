package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.R

data class YGChipButtonPreviewData(
    val startIconResource: Int?,
    val endIconResource: Int?,
    val colors: YGChipButtonColors,
)

class YGChipButtonPreviewParameterProvider : PreviewParameterProvider<YGChipButtonPreviewData> {
    override val values = sequenceOf(
        YGChipButtonPreviewData(
            startIconResource = null,
            endIconResource = null,
            colors = YGChipButtonColorsDefaults.CherrySolid,
        ),
        YGChipButtonPreviewData(
            startIconResource = R.drawable.ic_plus,
            endIconResource = null,
            colors = YGChipButtonColorsDefaults.GrayOutline,
        ),
        YGChipButtonPreviewData(
            startIconResource = null,
            endIconResource = R.drawable.ic_plus,
            colors = YGChipButtonColorsDefaults.CherrySolid,
        ),
    )
}
