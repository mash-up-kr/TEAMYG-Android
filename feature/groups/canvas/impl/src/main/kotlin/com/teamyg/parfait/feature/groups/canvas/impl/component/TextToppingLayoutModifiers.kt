package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val TEXT_TOPPING_LINE_HEIGHT_RATIO = 1.4f

/** 글자 크기에 맞춰 행간을 건다. */
internal fun TextStyle.withTextToppingSizing(fontSizeSp: Float): TextStyle = copy(
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * TEXT_TOPPING_LINE_HEIGHT_RATIO).sp,
)

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

/**
 * 박스가 [maxWidthPx]×[maxHeightPx] 를 넘으면 전체가 다 들어오게 비율 축소한다.
 * 편집 중엔 [maxWidthPx] 를 무한대로 넘겨 높이만 제한하고, 배치 모드에선 캔버스 너비·높이를 모두 제한한다.
 */
internal fun Modifier.scaleBoxToFit(
    maxWidthPx: Int,
    maxHeightPx: Int,
): Modifier = layout { measurable, constraints ->

    val placeable = measurable.measure(Constraints())
    val fits = (maxWidthPx <= 0 || placeable.width <= maxWidthPx) &&
        (maxHeightPx <= 0 || placeable.height <= maxHeightPx)
    if (fits) {
        return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
    val widthScale = if (maxWidthPx > 0) maxWidthPx.toFloat() / placeable.width else 1f
    val heightScale = if (maxHeightPx > 0) maxHeightPx.toFloat() / placeable.height else 1f
    val scale = minOf(widthScale, heightScale)
    val scaledWidth = (placeable.width * scale).roundToInt()
    val scaledHeight = (placeable.height * scale).roundToInt()
    layout(scaledWidth, scaledHeight) {
        placeable.placeRelativeWithLayer(
            x = (scaledWidth - placeable.width) / 2,
            y = 0,
        ) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
    }
}
