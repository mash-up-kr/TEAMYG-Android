package com.teamyg.parfait.data.source.parfaitimage.mapper

import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageBorderRequest
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageBorderResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.UpdateParfaitImageResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO

private const val BORDER_TYPE_NONE = "NONE"
private const val BORDER_TYPE_SOLID = "SOLID"

/**
 * sealed 테두리를 서버가 받는 평면 3필드(타입, 색, 두께)로 편다. None 이면 색·두께를 보내지 않는다.
 */
private fun ToppingBorder.flatten(): Triple<String, String?, Double?> = when (this) {
    ToppingBorder.None -> Triple(BORDER_TYPE_NONE, null, null)
    is ToppingBorder.Solid -> Triple(BORDER_TYPE_SOLID, color, width)
}

internal fun ToppingTransform.toPlaceRequest(
    imageId: ImageId,
    border: ToppingBorder,
): PlaceParfaitImageRequest {
    val (borderType, borderColor, borderWidth) = border.flatten()
    return PlaceParfaitImageRequest(
        imageId = imageId.value,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
    )
}

internal fun PlaceParfaitImageResponse.toPlacedToppingVO(): PlacedToppingVO = PlacedToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    placedBy = placedBy.toToppingPlacerVO(),
)

private fun PlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
)

internal fun UpdateParfaitImageResponse.toUpdatedToppingVO(): UpdatedToppingVO = UpdatedToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
)

internal fun ToppingBorder.toUpdateBorderRequest(): UpdateParfaitImageBorderRequest {
    val (borderType, borderColor, borderWidth) = flatten()
    return UpdateParfaitImageBorderRequest(
        borderType = borderType,
        borderColor = borderColor,
        borderWidth = borderWidth,
    )
}

internal fun UpdateParfaitImageBorderResponse.toUpdatedToppingBorderVO(): UpdatedToppingBorderVO =
    UpdatedToppingBorderVO(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        border = toToppingBorder(),
    )

/**
 * SOLID 인데 색이나 두께가 비어 있으면 Solid 를 만들 수 없으므로 None 으로 떨어뜨린다.
 */
private fun UpdateParfaitImageBorderResponse.toToppingBorder(): ToppingBorder {
    if (borderType != BORDER_TYPE_SOLID) return ToppingBorder.None
    val color = borderColor ?: return ToppingBorder.None
    val width = borderWidth ?: return ToppingBorder.None
    return ToppingBorder.Solid(color = color, width = width)
}
