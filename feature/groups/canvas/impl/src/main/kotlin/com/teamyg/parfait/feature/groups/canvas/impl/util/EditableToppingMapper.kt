package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType

internal fun CanvasToppingVO.toEditableTopping(): EditableTopping = EditableTopping(
    parfaitImageId = parfaitImageId.value,
    isMine = isMine,
    imageUrl = imageUrl,
    positionX = transform.positionX.toFloat(),
    positionY = transform.positionY.toFloat(),
    positionZ = transform.positionZ,
    scale = transform.scale.toFloat(),
    rotationDegrees = transform.rotation.toFloat(),
    border = border.toToppingBorderStyleOrNull(),
)

internal fun CanvasTextToppingVO.toEditableText(): EditableText = EditableText(
    parfaitTextId = parfaitTextId.value,
    isMine = isMine,
    text = text,
    color = textToppingColorOf(style.color),
    type = TextToppingType.of(isFill = style.isFill, isStroke = style.isStroke),
    designFontSize = style.fontSize.toFloat(),
    positionX = transform.positionX.toFloat(),
    positionY = transform.positionY.toFloat(),
    positionZ = transform.positionZ,
    scale = transform.scale.toFloat(),
    rotationDegrees = transform.rotation.toFloat(),
)
