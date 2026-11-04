package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

private val DEFAULT_FONT_SIZE = 14.sp

/**
 * sp 값을 피그마 dp로 보고, 시스템 글자 크기(`fontScale`)를 곱해도 그 dp 크기로 그려지는 sp로 되돌린다.
 * 멱등이 아니므로 이미 변환한 값을 다시 넘기지 않는다. 래퍼에는 원본 스타일을 넘긴다.
 */
internal fun TextUnit.toFixedFontScale(density: Density): TextUnit =
    if (isSp) with(density) { value.dp.toSp() } else this

/**
 * 렌더러가 sp lineHeight를 글자 크기 대비 비율로 그리므로(비선형 스케일링), lineHeight는 비율을 유지하도록 바꾼다.
 * `textIndent`는 변환하지 않는다.
 *
 * fontSize가 Unspecified면 먼저 14.sp로 채운다. 렌더러와 TextMeasurer가 둘 다 14sp로 떨어지는데, 그대로 두면 fontScale을 따라 커진다.
 */
fun TextStyle.toFixedFontScale(density: Density): TextStyle {
    val sourceFontSize = if (fontSize.isUnspecified) DEFAULT_FONT_SIZE else fontSize
    val fixedFontSize = sourceFontSize.toFixedFontScale(density)
    val fixedLineHeight = if (lineHeight.isSp && sourceFontSize.isSp) {
        (lineHeight.value * fixedFontSize.value / sourceFontSize.value).sp
    } else {
        lineHeight.toFixedFontScale(density)
    }
    return copy(
        fontSize = fixedFontSize,
        lineHeight = fixedLineHeight,
        letterSpacing = letterSpacing.toFixedFontScale(density),
    )
}

/**
 * [ParagraphStyle]의 lineHeight는 기준 fontSize를 몰라 절대값으로 변환한다. 비선형 구간에서 줄 간격이 어긋날 수 있다.
 */
fun AnnotatedString.toFixedFontScale(density: Density): AnnotatedString = mapAnnotations { range ->
    val item: AnnotatedString.Annotation =
        when (val annotation = range.item) {
            is SpanStyle -> annotation.toFixedFontScale(density)

            is ParagraphStyle ->
                annotation.copy(lineHeight = annotation.lineHeight.toFixedFontScale(density))

            is LinkAnnotation.Url ->
                annotation.copy(styles = annotation.styles?.toFixedFontScale(density))

            is LinkAnnotation.Clickable ->
                annotation.copy(styles = annotation.styles?.toFixedFontScale(density))

            else -> annotation
        }
    AnnotatedString.Range(item, range.start, range.end, range.tag)
}

private fun SpanStyle.toFixedFontScale(density: Density): SpanStyle = copy(
    fontSize = fontSize.toFixedFontScale(density),
    letterSpacing = letterSpacing.toFixedFontScale(density),
)

private fun TextLinkStyles.toFixedFontScale(density: Density): TextLinkStyles = TextLinkStyles(
    style = style?.toFixedFontScale(density),
    focusedStyle = focusedStyle?.toFixedFontScale(density),
    hoveredStyle = hoveredStyle?.toFixedFontScale(density),
    pressedStyle = pressedStyle?.toFixedFontScale(density),
)
