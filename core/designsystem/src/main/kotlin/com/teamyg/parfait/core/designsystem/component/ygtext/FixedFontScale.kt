package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * sp를 시스템 글자 크기와 무관하게 디자인 dp 값으로 그려지는 sp로 바꾼다.
 * 멱등이 아니므로 이미 변환한 값을 다시 넘기지 않는다. 래퍼에는 원본 스타일을 넘긴다.
 */
fun TextUnit.toFixedFontScale(density: Density): TextUnit = if (isSp) with(density) { value.dp.toSp() } else this

/**
 * 렌더러가 sp lineHeight를 글자 크기 대비 비율로 그리므로(비선형 스케일링), lineHeight는 비율을 유지하도록 바꾼다.
 * sp fontSize를 모르는 lineHeight(예: [AnnotatedString] 안의 [ParagraphStyle])는 절대값 변환으로 떨어져 비선형 구간에서 어긋난다.
 */
fun TextStyle.toFixedFontScale(density: Density): TextStyle {
    val fixedFontSize = fontSize.toFixedFontScale(density)
    val fixedLineHeight = if (lineHeight.isSp && fontSize.isSp) {
        (lineHeight.value * fixedFontSize.value / fontSize.value).sp
    } else {
        lineHeight.toFixedFontScale(density)
    }
    return copy(
        fontSize = fixedFontSize,
        lineHeight = fixedLineHeight,
        letterSpacing = letterSpacing.toFixedFontScale(density),
    )
}

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

@Composable
@ReadOnlyComposable
fun TextStyle.toFixedFontScale(): TextStyle = toFixedFontScale(LocalDensity.current)
