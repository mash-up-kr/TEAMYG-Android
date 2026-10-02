package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping

internal fun CanvasToppingVO.toEditableTopping(): EditableTopping = EditableTopping(
    parfaitImageId = parfaitImageId.value,
    isMine = isMine,
    imageUrl = imageUrl,
    positionX = transform.positionX.toFloat(),
    positionY = transform.positionY.toFloat(),
    scale = transform.scale.toFloat(),
    rotationDegrees = transform.rotation.toFloat(),
    border = border.toToppingBorderStyleOrNull(),
)
