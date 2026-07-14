package com.teamyg.parfait.core.designsystem.component.ygchip

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

data class YGChipPreviewData(
    val fillColor: Color,
    val strokeColor: Color,
    val textColor: Color,
)

class YGChipPreviewParameterProvider : PreviewParameterProvider<YGChipPreviewData> {
    override val values = sequenceOf(
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry100,
            strokeColor = YGAtomicColors.Cherry.Cherry50,
            textColor = YGAtomicColors.Cherry.Cherry300,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry200,
            strokeColor = YGAtomicColors.Cherry.Cherry100,
            textColor = YGAtomicColors.Melon.Melon500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry200,
            strokeColor = YGAtomicColors.Cherry.Cherry100,
            textColor = YGAtomicColors.Melon.Melon500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry300,
            strokeColor = YGAtomicColors.Cherry.Cherry50,
            textColor = YGAtomicColors.Pudding.Pudding500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry500,
            strokeColor = YGAtomicColors.Cherry.Cherry100,
            textColor = YGAtomicColors.Cherry.Cherry100,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Cherry.Cherry700,
            strokeColor = YGAtomicColors.Cherry.Cherry200,
            textColor = YGAtomicColors.Gray.White,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Gray.White,
            strokeColor = YGAtomicColors.Cherry.Cherry200,
            textColor = YGAtomicColors.Melon.Melon500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Gray.Gray200,
            strokeColor = YGAtomicColors.Cherry.Cherry200,
            textColor = YGAtomicColors.Pudding.Pudding500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Melon.Melon500,
            strokeColor = YGAtomicColors.Cherry.Cherry50,
            textColor = YGAtomicColors.Pudding.Pudding500,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Melon.Melon500,
            strokeColor = YGAtomicColors.Cherry.Cherry100,
            textColor = YGAtomicColors.Cherry.Cherry300,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Pudding.Pudding500,
            strokeColor = YGAtomicColors.Melon.Melon500,
            textColor = YGAtomicColors.Cherry.Cherry300,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Pudding.Pudding500,
            strokeColor = YGAtomicColors.Cherry.Cherry100,
            textColor = YGAtomicColors.Cherry.Cherry300,
        ),
        YGChipPreviewData(
            fillColor = YGAtomicColors.Gray.White,
            strokeColor = YGAtomicColors.Gray.Gray100,
            textColor = YGAtomicColors.Gray.Gray900,
        ),
    )
}
