package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle

inline fun <R : Any> Builder.withStyle(
    textStyle: TextStyle,
    block: Builder.() -> R,
): R = withStyle(
    style = textStyle.toSpanStyle(),
    block = block,
)
