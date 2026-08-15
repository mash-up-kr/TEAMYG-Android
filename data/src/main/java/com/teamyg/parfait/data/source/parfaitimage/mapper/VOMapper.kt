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
 * sealed 테두리를 서버가 받는 평면 3필드로 편다. None 이면 색·두께를 보내지 않는다.
 */
internal fun ToppingTransform.toPlaceRequest(
    imageId: ImageId,
    border: ToppingBorder,
): PlaceParfaitImageRequest {
    val solid = border as? ToppingBorder.Solid
    return PlaceParfaitImageRequest(
        imageId = imageId.value,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
        borderType = when (border) {
            ToppingBorder.None -> BORDER_TYPE_NONE
            is ToppingBorder.Solid -> BORDER_TYPE_SOLID
        },
        borderColor = solid?.color,
        borderWidth = solid?.width,
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
    val solid = this as? ToppingBorder.Solid
    return UpdateParfaitImageBorderRequest(
        borderType = when (this) {
            ToppingBorder.None -> BORDER_TYPE_NONE
            is ToppingBorder.Solid -> BORDER_TYPE_SOLID
        },
        borderColor = solid?.color,
        borderWidth = solid?.width,
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
