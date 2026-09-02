package com.teamyg.parfait.core.designsystem.theme.typography

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal object YGTypographyDefaults {
    //region title
    private val T01_B = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )

    private val T01_SB = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )

    private val T02_B = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )

    private val T02_SB = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )

    private val T03_B = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )

    private val T03_SB = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = (1.4).em,
        letterSpacing = (-0.011).em,
    )
    //endregion

    //region body
    private val B01_B = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val B01_SB = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val B01_R = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val B02_B = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val B02_SB = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val B02_R = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )
    //endregion

    //region caption
    private val C01_M = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )

    private val C01_R = TextStyle(
        fontFamily = YGFontFamily.suitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = (1.5).em,
        letterSpacing = (-0.011).em,
    )
    //endregion

    //region Typography
    private val YGDefaultTypographyTitle: YGTypographyTitle = YGTypographyTitle(
        t01B = T01_B,
        t01SB = T01_SB,
        t02B = T02_B,
        t02SB = T02_SB,
        t03B = T03_B,
        t03SB = T03_SB,
    )

    private val YGDefaultTypographyBody: YGTypographyBody = YGTypographyBody(
        b01B = B01_B,
        b01SB = B01_SB,
        b01R = B01_R,
        b02B = B02_B,
        b02SB = B02_SB,
        b02R = B02_R,
    )

    private val YGDefaultTypographyCaption: YGTypographyCaption = YGTypographyCaption(
        c01M = C01_M,
        c01R = C01_R,
    )

    val YGDefaultTypography: YGTypography = YGTypography(
        title = YGDefaultTypographyTitle,
        body = YGDefaultTypographyBody,
        caption = YGDefaultTypographyCaption,
    )
    //endregion
}
