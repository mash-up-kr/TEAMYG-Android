package com.teamyg.parfait.core.designsystem.component.yggrouptagchip

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * Figma Grouptag-Chip Type
 */
enum class YGGrouptagChipType(val timestampColor: Color) {
    TYPE_1_2(YGAtomicColors.Cherry.Cherry100),
    TYPE_3_4(YGAtomicColors.Cherry.Cherry200),
    TYPE_5_6(YGAtomicColors.Cherry.Cherry300),
    TYPE_7_8(YGAtomicColors.Gray.Gray200),
    TYPE_9_10(YGAtomicColors.Melon.Melon500),
    TYPE_11_12(YGAtomicColors.Pudding.Pudding500),
}
