package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGChipButtonColorsDefaults {
    val CherryBorderPressed: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray600,
        pressedForegroundColor = YGAtomicColors.Gray.Gray700,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry50,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry50,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Cherry.Cherry100,
    )
    val CherryBackgroundPressed: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray950,
        pressedForegroundColor = YGAtomicColors.Gray.Gray950,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry200,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
    )
}
