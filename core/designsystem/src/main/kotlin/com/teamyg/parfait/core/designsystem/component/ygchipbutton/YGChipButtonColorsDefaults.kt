package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGChipButtonColorsDefaults {
    val Cherry50BackgroundPressed: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray600,
        pressedForegroundColor = YGAtomicColors.Gray.Gray700,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry50,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
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
