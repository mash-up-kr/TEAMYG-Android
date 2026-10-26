package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType

private const val RGB_MASK = 0xFFFFFF

/** 서버에 저장되는 `#RRGGBB` 대문자 표기. 글자색([TextToppingColor.contentColor])을 보낸다 */
internal fun TextToppingColor.toWireColor(): String = "#%06X".format(contentColor.toArgb() and RGB_MASK)

/** 다른 클라이언트가 넣은 값은 형식이 제각각일 수 있어, 모르는 값은 [TextToppingColor.Black] 으로 읽는다 */
internal fun textToppingColorOf(wire: String): TextToppingColor =
    TextToppingColor.entries.firstOrNull { it.toWireColor().equals(wire, ignoreCase = true) }
        ?: TextToppingColor.Black

/** (isFill, isStroke) */
internal fun TextToppingType.toFillStroke(): Pair<Boolean, Boolean> = hasFill to hasBorder

/** 서버에 `isFill=false, isStroke=true` 도 저장될 수 있어, 배경이 없으면 테두리와 무관하게 [TextToppingType.DEFAULT] 다 */
internal fun textToppingTypeOf(
    isFill: Boolean,
    isStroke: Boolean,
): TextToppingType = when {
    !isFill -> TextToppingType.DEFAULT
    isStroke -> TextToppingType.FILL_STROKE
    else -> TextToppingType.FILL
}

internal fun textToppingStyleOf(
    color: TextToppingColor,
    type: TextToppingType,
    designFontSize: Float,
): TextToppingStyle {
    val (isFill, isStroke) = type.toFillStroke()
    return TextToppingStyle(
        color = color.toWireColor(),
        fontSize = designFontSize.toDouble(),
        isFill = isFill,
        isStroke = isStroke,
    )
}

/**
 * 박스 좌상단 Dp 오프셋을 중심 비율로 바꾼다. 이미지 쪽 [toToppingTransform] 과 달리
 * `scale`·`rotation` 은 기준이 같아 그대로 보낸다.
 */
internal fun toTextToppingTransform(
    offsetX: Dp,
    offsetY: Dp,
    boxSize: DpSize,
    canvasSize: DpSize,
    scale: Float,
    rotationDegrees: Float,
    positionZ: Int,
): ToppingTransform = ToppingTransform(
    positionX = ((offsetX.value + boxSize.width.value / 2) / canvasSize.width.value).toDouble(),
    positionY = ((offsetY.value + boxSize.height.value / 2) / canvasSize.height.value).toDouble(),
    positionZ = positionZ,
    scale = scale.toDouble(),
    rotation = rotationDegrees.toDouble(),
)
