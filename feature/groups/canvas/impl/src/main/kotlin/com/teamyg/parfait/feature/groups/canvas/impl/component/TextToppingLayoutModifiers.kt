package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val TEXT_TOPPING_LINE_HEIGHT_RATIO = 1.4f

internal fun TextStyle.withTextToppingSizing(fontSizeSp: Float): TextStyle = copy(
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * TEXT_TOPPING_LINE_HEIGHT_RATIO).sp,
)

internal class TextToppingAutoFit(
    val fontSizeSp: Float,
    val fieldMaxWidthDp: Dp,
    val isOverflowing: Boolean,
)

/** 세로 예산을 넘으면 글자 크기와 래핑 폭을 같은 비율로 줄인다 — 줄바꿈 위치는 그대로다 */
internal fun fitTextToppingToHeight(
    textMeasurer: TextMeasurer,
    text: AnnotatedString,
    baseStyle: TextStyle,
    maxWidthPx: Int,
    maxWidthDp: Dp,
    maxHeightPx: Int,
    maxFontSizeSp: Float,
    density: Density,
): TextToppingAutoFit {
    val naturalHeightPx = textMeasurer
        .measure(
            text = text,
            style = baseStyle.withTextToppingSizing(maxFontSizeSp),
            constraints = Constraints(maxWidth = maxWidthPx),
        ).size.height

    var scale = (maxHeightPx.toFloat() / naturalHeightPx).coerceAtMost(1f)
    var fontSizeSp = maxFontSizeSp * scale
    var fieldMaxWidthDp = maxWidthDp * scale

    val measuredHeightPx = textMeasurer
        .measure(
            text = text,
            style = baseStyle.withTextToppingSizing(fontSizeSp),
            constraints = Constraints(maxWidth = with(density) { fieldMaxWidthDp.roundToPx() }),
        ).size.height
    if (measuredHeightPx > maxHeightPx) {
        scale *= maxHeightPx.toFloat() / measuredHeightPx
        fontSizeSp = maxFontSizeSp * scale
        fieldMaxWidthDp = maxWidthDp * scale
    }

    return TextToppingAutoFit(
        fontSizeSp = fontSizeSp,
        fieldMaxWidthDp = fieldMaxWidthDp,
        isOverflowing = naturalHeightPx > maxHeightPx,
    )
}

/** 편집↔배치 전환용 세로 배치. */
internal fun Modifier.editingTopAnchored(
    arrangeCenter: DpOffset,
    editTopPx: Int,
    canvasWidthPx: Int,
    focusFraction: Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val arrangeLeft = arrangeCenter.x.roundToPx() - placeable.width / 2
        val arrangeTop = arrangeCenter.y.roundToPx() - placeable.height / 2
        val editLeft = (canvasWidthPx - placeable.width) / 2
        placeable.placeRelative(
            x = arrangeLeft + ((editLeft - arrangeLeft) * focusFraction).roundToInt(),
            y = arrangeTop + ((editTopPx - arrangeTop) * focusFraction).roundToInt(),
        )
    }
}

/** 배경 여백으로 박스가 캔버스보다 넓어져도 래핑 폭이 바뀌지 않게 제약 없이 잰다. 넘친 부분은 캔버스가 자른다 */
internal fun Modifier.measureTextBoxUnbounded(): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints())
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}
