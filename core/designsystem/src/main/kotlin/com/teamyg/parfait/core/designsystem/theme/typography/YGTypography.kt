package com.teamyg.parfait.core.designsystem.theme.typography

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object YGTypography {
    object Title {
        val T01_B = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )

        val T01_SB = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )

        val T02_B = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )

        val T02_SB = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )

        val T03_B = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )

        val T03_SB = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = (1.4).em,
            letterSpacing = (-0.011).em,
        )
    }

    object Body {
        val B01_B = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val B01_SB = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val B01_R = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val B02_B = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val B02_SB = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val B02_R = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )
    }

    object Caption {
        val C01_M = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )

        val C01_R = TextStyle(
            fontFamily = YGFontFamily.suitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = (1.5).em,
            letterSpacing = (-0.011).em,
        )
    }
}
