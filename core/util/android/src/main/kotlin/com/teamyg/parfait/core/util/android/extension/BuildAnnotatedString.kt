package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle

inline fun <R : Any> Builder.withStyle(textStyle: TextStyle, block: Builder.() -> R): R = withStyle(
    style = SpanStyle(
        color = textStyle.color,
        fontSize = textStyle.fontSize,
        fontWeight = textStyle.fontWeight,
        fontStyle = textStyle.fontStyle,
        fontSynthesis = textStyle.fontSynthesis,
        fontFamily = textStyle.fontFamily,
        fontFeatureSettings = textStyle.fontFeatureSettings,
        letterSpacing = textStyle.letterSpacing,
        baselineShift = textStyle.baselineShift,
        textGeometricTransform = textStyle.textGeometricTransform,
        localeList = textStyle.localeList,
        background = textStyle.background,
        textDecoration = textStyle.textDecoration,
        shadow = textStyle.shadow,
    ),
    block = block,
)
