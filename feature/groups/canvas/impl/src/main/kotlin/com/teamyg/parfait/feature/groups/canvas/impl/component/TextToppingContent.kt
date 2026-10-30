package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_DESIGN_CANVAS_WIDTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_FONT_SIZE_SP
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingScript
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingAnnotatedString
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingFontSizeSp
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingWrapWidth

/**
 * 편집 필드와 읽기 전용 렌더러가 같은 값을 써야 줄바꿈이 맞는다.
 *
 * 글꼴·자간은 placeholder 처럼 구간이 없는 텍스트를 위해 [TextToppingScript.DEFAULT] 구간 스타일을 깐다.
 */
@Composable
@ReadOnlyComposable
internal fun textToppingBaseStyle(): TextStyle = YGTheme.typography.body.b01R
    .merge(TextToppingScript.DEFAULT.spanStyle)

/**
 * C-106 배치 모드가 키보드를 내린 뒤 보여 주는 크기·줄바꿈·박스와 같아야 한다.
 * 위치·scale·rotation 은 호출부 몫이다.
 */
@Composable
internal fun TextToppingContent(
    text: String,
    color: TextToppingColor,
    type: TextToppingType,
    align: TextAlign,
    designFontSize: Float,
    canvasWidth: Dp,
    modifier: Modifier = Modifier,
) {
    // 시스템 폰트 크기(접근성) 설정과 무관하게 모든 사용자에게 같은 크기·줄바꿈으로 보여야 한다
    val systemDensity = LocalDensity.current
    val fixedFontScaleDensity = remember(systemDensity) {
        Density(density = systemDensity.density, fontScale = 1f)
    }

    CompositionLocalProvider(LocalDensity provides fixedFontScaleDensity) {
        val fontSizeSp = textToppingFontSizeSp(designFontSize = designFontSize, canvasWidth = canvasWidth)

        TextToppingBackground(
            type = type,
            color = color,
            fontSizeSp = fontSizeSp,
            modifier = modifier.measureTextBoxUnbounded(),
        ) {
            // 너비 상한만 주면 Text 는 한 줄 자연 폭과 상한 중 작은 쪽으로 감싼다 — C-106 의 hug 폭과 같다
            BasicText(
                text = remember(text) { textToppingAnnotatedString(text) },
                style = textToppingBaseStyle()
                    .withTextToppingSizing(fontSizeSp)
                    .copy(color = color.contentColor, textAlign = align),
                modifier = Modifier.widthIn(
                    max = textToppingWrapWidth(designFontSize = designFontSize, canvasWidth = canvasWidth),
                ),
            )
        }
    }
}

@YGPreview
@Composable
private fun TextToppingContentPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5)) {
        TextToppingType.entries.forEach { type ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("파르페", "오늘 다 같이\n먹은 파르페").forEach { text ->
                    TextToppingContent(
                        text = text,
                        color = TextToppingColor.Pink,
                        type = type,
                        align = TextAlign.Center,
                        designFontSize = TEXT_TOPPING_FONT_SIZE_SP / 2,
                        canvasWidth = TEXT_TOPPING_DESIGN_CANVAS_WIDTH,
                    )
                }
            }
        }
    }
}
