package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * sp를 시스템 글자 크기와 무관하게 디자인 dp 값으로 그려지는 sp로 바꾼다.
 * 멱등이 아니므로 이미 변환한 값을 다시 넘기지 않는다. 래퍼에는 원본 스타일을 넘긴다.
 */
fun TextUnit.toFixedFontScale(density: Density): TextUnit = if (isSp) with(density) { value.dp.toSp() } else this

fun TextStyle.toFixedFontScale(density: Density): TextStyle = copy(
    fontSize = fontSize.toFixedFontScale(density),
    lineHeight = lineHeight.toFixedFontScale(density),
    letterSpacing = letterSpacing.toFixedFontScale(density),
)

fun AnnotatedString.toFixedFontScale(density: Density): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    spanStyles.forEach { range ->
        val style = range.item
        builder.addStyle(
            style.copy(
                fontSize = style.fontSize.toFixedFontScale(density),
                letterSpacing = style.letterSpacing.toFixedFontScale(density),
            ),
            range.start,
            range.end,
        )
    }
    paragraphStyles.forEach { range ->
        builder.addStyle(
            range.item.copy(lineHeight = range.item.lineHeight.toFixedFontScale(density)),
            range.start,
            range.end,
        )
    }
    // 스타일이 아닌 annotation은 공개 API로 하나씩 옮긴다
    getStringAnnotations(0, length).forEach {
        builder.addStringAnnotation(it.tag, it.item, it.start, it.end)
    }
    getTtsAnnotations(0, length).forEach { builder.addTtsAnnotation(it.item, it.start, it.end) }
    getLinkAnnotations(0, length).forEach {
        when (val link = it.item) {
            is LinkAnnotation.Url -> builder.addLink(link, it.start, it.end)
            is LinkAnnotation.Clickable -> builder.addLink(link, it.start, it.end)
        }
    }
    return builder.toAnnotatedString()
}

@Composable
@ReadOnlyComposable
fun TextStyle.toFixedFontScale(): TextStyle = toFixedFontScale(LocalDensity.current)
