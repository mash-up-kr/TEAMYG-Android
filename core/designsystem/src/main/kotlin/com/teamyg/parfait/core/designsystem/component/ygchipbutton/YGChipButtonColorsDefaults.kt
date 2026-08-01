package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGChipButtonColorsDefaults {
    /**
     * Figma Button-Chip-Left
     */
    val CherrySubtle: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray600,
        pressedForegroundColor = YGAtomicColors.Gray.Gray700,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry50,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
    )

    /**
     * Figma Button-Chip-Right
     */
    val CherrySolid: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray950,
        pressedForegroundColor = YGAtomicColors.Gray.Gray950,
        defaultBackgroundColor = YGAtomicColors.Cherry.Cherry100,
        pressedBackgroundColor = YGAtomicColors.Cherry.Cherry200,
        defaultBorderColor = YGAtomicColors.Gray.Transparent,
        pressedBorderColor = YGAtomicColors.Gray.Transparent,
    )
}
