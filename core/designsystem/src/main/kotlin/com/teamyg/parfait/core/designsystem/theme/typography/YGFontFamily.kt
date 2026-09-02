package com.teamyg.parfait.core.designsystem.theme.typography

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.teamyg.parfait.core.designsystem.R

internal object YGFontFamily {
    val suitFontFamily: FontFamily = FontFamily(
        Font(R.font.suit_regular, FontWeight.Normal),
        Font(R.font.suit_medium, FontWeight.Medium),
        Font(R.font.suit_semi_bold, FontWeight.SemiBold),
        Font(R.font.suit_bold, FontWeight.Bold),
    )
}
