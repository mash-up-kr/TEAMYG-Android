package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * 텍스트 토핑 8색.
 */
enum class TextToppingColor(
    val stroke: Color,
    val fill: Color,
) {
    White(stroke = YGAtomicColors.Gray.White, fill = YGAtomicColors.Gray.Black),
    Black(stroke = YGAtomicColors.Gray.Black, fill = YGAtomicColors.Gray.White),
    Pink(stroke = YGAtomicColors.Pink.Stroke, fill = YGAtomicColors.Pink.Fill),
    Orange(stroke = YGAtomicColors.Orange.Stroke, fill = YGAtomicColors.Orange.Fill),
    Yellow(stroke = YGAtomicColors.Yellow.Stroke, fill = YGAtomicColors.Yellow.Fill),
    Green(stroke = YGAtomicColors.Green.Stroke, fill = YGAtomicColors.Green.Fill),
    Blue(stroke = YGAtomicColors.Blue.Stroke, fill = YGAtomicColors.Blue.Fill),
    Violet(stroke = YGAtomicColors.Violet.Stroke, fill = YGAtomicColors.Violet.Fill),
}
