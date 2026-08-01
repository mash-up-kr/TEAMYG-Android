package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGChipButtonColorsDefaults {
    /**
     * Figma Button-Chip-Left
     */
    val GrayOutline: YGChipButtonColors = YGChipButtonColors(
        defaultForegroundColor = YGAtomicColors.Gray.Gray900,
        pressedForegroundColor = YGAtomicColors.Gray.Gray950,
        defaultBackgroundColor = YGAtomicColors.Gray.White,
        pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
        defaultBorderColor = YGAtomicColors.Gray.Gray500,
        pressedBorderColor = YGAtomicColors.Gray.Gray500,
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
