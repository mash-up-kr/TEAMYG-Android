package com.teamyg.parfait.data.source.topping.mapper

import com.teamyg.parfait.data.service.model.request.parfaittext.PlaceParfaitTextRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.UpdateParfaitTextRequest
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitTextResponse
import com.teamyg.parfait.data.service.model.response.parfaittext.UpdateParfaitTextResponse
import com.teamyg.parfait.data.source.common.mapper.toTextToppingAlign
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
    textAlign = style.textAlign.name,
)

internal fun TodayParfaitTextResponse.toPlacedTextToppingVO(): PlacedTextToppingVO = PlacedTextToppingVO(
    parfaitTextId = ParfaitTextId(parfaitTextId),
    text = text,
    transform = transformOf(positionX, positionY, positionZ, scale, rotation),
    style = styleOf(color, fontSize, isFill, isStroke, textAlign),
    placedBy = placedBy.toToppingPlacerVO(),
)

private fun PlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
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
    textAlign = textAlign?.name,
)

internal fun UpdateParfaitTextResponse.toUpdatedTextToppingVO(): UpdatedTextToppingVO = UpdatedTextToppingVO(
    parfaitTextId = ParfaitTextId(parfaitTextId),
    text = text,
    transform = transformOf(positionX, positionY, positionZ, scale, rotation),
    style = styleOf(color, fontSize, isFill, isStroke, textAlign),
)

private fun transformOf(
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

private fun styleOf(
    color: String,
    fontSize: Double,
    isFill: Boolean,
    isStroke: Boolean,
    textAlign: String,
): TextToppingStyle = TextToppingStyle(
    color = color,
    fontSize = fontSize,
    isFill = isFill,
    isStroke = isStroke,
    textAlign = textAlign.toTextToppingAlign(),
)
