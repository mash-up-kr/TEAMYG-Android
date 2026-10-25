package com.teamyg.parfait.data.source.topping.mapper

import com.teamyg.parfait.data.service.model.request.parfaittext.PlaceParfaitTextRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.UpdateParfaitTextRequest
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitTextResponse
import com.teamyg.parfait.data.service.model.response.parfaittext.UpdateParfaitTextResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO

internal fun ToppingTransform.toPlaceTextRequest(
    text: String,
    style: TextToppingStyle,
): PlaceParfaitTextRequest = PlaceParfaitTextRequest(
    text = text,
    positionX = positionX,
    positionY = positionY,
    positionZ = positionZ,
    scale = scale,
    rotation = rotation,
    color = style.color,
    fontSize = style.fontSize,
    isFill = style.isFill,
    isStroke = style.isStroke,
)

internal fun TodayParfaitTextResponse.toPlacedTextToppingVO(): PlacedTextToppingVO = PlacedTextToppingVO(
    parfaitTextId = ParfaitTextId(parfaitTextId),
    text = text,
    transform = toTransform(positionX, positionY, positionZ, scale, rotation),
    style = toStyle(color, fontSize, isFill, isStroke),
    placedBy = ToppingPlacerVO(
        groupMemberId = GroupMemberId(placedBy.groupMemberId),
        nickname = GroupNickname(placedBy.nickname),
    ),
)

internal fun TextToppingUpdate.toUpdateTextRequest(): UpdateParfaitTextRequest = UpdateParfaitTextRequest(
    positionX = positionX,
    positionY = positionY,
    positionZ = positionZ,
    scale = scale,
    rotation = rotation,
    color = color,
    fontSize = fontSize,
    isFill = isFill,
    isStroke = isStroke,
)

internal fun UpdateParfaitTextResponse.toUpdatedTextToppingVO(): UpdatedTextToppingVO = UpdatedTextToppingVO(
    parfaitTextId = ParfaitTextId(parfaitTextId),
    text = text,
    transform = toTransform(positionX, positionY, positionZ, scale, rotation),
    style = toStyle(color, fontSize, isFill, isStroke),
)

private fun toTransform(
    positionX: Double,
    positionY: Double,
    positionZ: Int,
    scale: Double,
    rotation: Double,
): ToppingTransform = ToppingTransform(
    positionX = positionX,
    positionY = positionY,
    positionZ = positionZ,
    scale = scale,
    rotation = rotation,
)

private fun toStyle(
    color: String,
    fontSize: Double,
    isFill: Boolean,
    isStroke: Boolean,
): TextToppingStyle = TextToppingStyle(
    color = color,
    fontSize = fontSize,
    isFill = isFill,
    isStroke = isStroke,
)
