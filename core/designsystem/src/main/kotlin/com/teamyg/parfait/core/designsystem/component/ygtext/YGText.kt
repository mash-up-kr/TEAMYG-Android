package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * 시스템 글자 크기와 무관하게 피그마 dp 값 그대로 그리는 `material3.Text`.
 * 파라미터는 원본과 같고 `fontSize`·`letterSpacing`·`lineHeight`를 `style`에 합쳐 한 번만 변환한다.
 * `autoSize`는 변환하지 않는다.
 */
@Composable
fun YGText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    val density = LocalDensity.current
    // lineHeight 비율 변환에 fontSize가 필요해 파라미터를 style에 먼저 합친 뒤 한 번만 변환한다
    val fixedStyle = remember(style, fontSize, letterSpacing, lineHeight, density) {
        style
            .merge(
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
            ).toFixedFontScale(density)
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = autoSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = fixedStyle,
    )
}

/**
 * [AnnotatedString]을 받는 [YGText]. `text` 안의 스팬·문단·링크 스타일도 함께 변환한다.
 */
@Composable
fun YGText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val density = LocalDensity.current
    val fixedText = remember(text, density) { text.toFixedFontScale(density) }
    // lineHeight 비율 변환에 fontSize가 필요해 파라미터를 style에 먼저 합친 뒤 한 번만 변환한다
    val fixedStyle = remember(style, fontSize, letterSpacing, lineHeight, density) {
        style
            .merge(
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight,
            ).toFixedFontScale(density)
    }

    Text(
        text = fixedText,
        modifier = modifier,
        color = color,
        autoSize = autoSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = fixedStyle,
    )
}
