package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

enum class TextToppingColor(
    val contentColor: Color,
    val backgroundColor: Color,
) {
    White(contentColor = YGAtomicColors.Gray.White, backgroundColor = YGAtomicColors.Gray.Black),
    Black(contentColor = YGAtomicColors.Gray.Black, backgroundColor = YGAtomicColors.Gray.White),
    Pink(contentColor = Color(0xFFFA91A2), backgroundColor = Color(0xFFFFF9FA)),
    Orange(contentColor = Color(0xFFFFBB42), backgroundColor = Color(0xFFFFFBF3)),
    Yellow(contentColor = Color(0xFFFFD21F), backgroundColor = Color(0xFFFEFEF6)),
    Green(contentColor = Color(0xFF64DB88), backgroundColor = Color(0xFFF4FCF7)),
    Blue(contentColor = Color(0xFF6DBBF2), backgroundColor = Color(0xFFF6FBFE)),
    Violet(contentColor = Color(0xFFCEA8FC), backgroundColor = Color(0xFFFAF4FF)),
}
