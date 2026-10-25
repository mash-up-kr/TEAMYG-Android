package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType

private const val HORIZONTAL_MARGIN_RATIO = 1f / 3f
private const val VERTICAL_MARGIN_RATIO = 1f / 6f
private const val BORDER_WIDTH_RATIO = 1f / 24f
private val BORDER_WIDTH_MIN = 0.5.dp

/**
 * 네모배경·테두리. 여백·테두리는 글자 크기 비율 그대로 반올림하지 않는다(스펙 「네모배경」).
 * 호출부가 `fontScale = 1` 로 고정해 두므로 [fontSizeSp] 수치를 그대로 dp 로 쓴다.
 */
@Composable
internal fun TextToppingBackground(
    type: TextToppingType,
    color: TextToppingColor,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val marginHorizontal = (fontSizeSp * HORIZONTAL_MARGIN_RATIO).dp
    val marginVertical = (fontSizeSp * VERTICAL_MARGIN_RATIO).dp
    val borderWidth = (fontSizeSp * BORDER_WIDTH_RATIO).dp.coerceAtLeast(BORDER_WIDTH_MIN)

    // 바깥쪽 테두리 → 배경 → 여백 순서로 쌓아야 테두리가 박스 바깥에 그려진다
    Box(
        modifier = modifier
            .then(
                if (type.hasBorder) {
                    Modifier
                        .border(width = borderWidth, color = color.contentColor, shape = RectangleShape)
                        .padding(borderWidth)
                } else {
                    Modifier
                },
            ).then(
                if (type.hasFill) {
                    Modifier
                        .background(color = color.backgroundColor, shape = RectangleShape)
                        .padding(horizontal = marginHorizontal, vertical = marginVertical)
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/** 타입 3종(기본/배경/배경+테두리)의 네모배경·테두리를 한눈에 — 색은 Pink 예시. */
@YGPreview
@Composable
private fun TextToppingBackgroundPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5)) {
        TextToppingType.entries.forEach { type ->
            TextToppingBackground(
                type = type,
                color = TextToppingColor.Pink,
                fontSizeSp = 24f,
            ) {
                Text(
                    text = "가나다 Abc",
                    style = YGTheme.typography.body.b01R.copy(
                        color = TextToppingColor.Pink.contentColor,
                        fontSize = 24.sp,
                    ),
                )
            }
        }
    }
}
