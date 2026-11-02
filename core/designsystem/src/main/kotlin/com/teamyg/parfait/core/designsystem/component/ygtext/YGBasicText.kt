package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * 시스템 글자 크기와 무관하게 피그마 dp 값 그대로 그리는 `foundation.text.BasicText`.
 * 파라미터는 원본과 같고 `text`·`style`을 한 번만 변환한다. `autoSize`는 변환하지 않는다.
 */
@Composable
fun YGBasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    val density = LocalDensity.current
    val fixedText = remember(text, density) { text.toFixedFontScale(density) }
    val fixedStyle = remember(style, density) { style.toFixedFontScale(density) }

    BasicText(
        text = fixedText,
        modifier = modifier,
        style = fixedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        color = color,
        autoSize = autoSize,
    )
}
