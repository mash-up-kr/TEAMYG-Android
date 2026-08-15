package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.service.model.request.parfaitimage.UpdateParfaitImageRequest
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlaceRequest
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlacedToppingVO
import com.teamyg.parfait.data.source.parfaitimage.mapper.toUpdateBorderRequest
import com.teamyg.parfait.data.source.parfaitimage.mapper.toUpdatedToppingBorderVO
import com.teamyg.parfait.data.source.parfaitimage.mapper.toUpdatedToppingVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import javax.inject.Inject

class ParfaitImageRemoteDataSourceImpl @Inject constructor(
    private val parfaitImageService: ParfaitImageService,
    private val apiCaller: ApiCaller,
) : ParfaitImageRemoteDataSource {
    override suspend fun placeTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                request = transform.toPlaceRequest(imageId = imageId, border = border),
            )
        },
        transform = { it.toPlacedToppingVO() },
    )

    override suspend fun updateTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        positionX: Double?,
        positionY: Double?,
        positionZ: Int?,
        scale: Double?,
        rotation: Double?,
    ): Result<UpdatedToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                parfaitImageId = parfaitImageId.value,
                request = UpdateParfaitImageRequest(
                    positionX = positionX,
                    positionY = positionY,
                    positionZ = positionZ,
                    scale = scale,
                    rotation = rotation,
                ),
            )
        },
        transform = { it.toUpdatedToppingVO() },
    )

    override suspend fun updateToppingBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                parfaitImageId = parfaitImageId.value,
                request = border.toUpdateBorderRequest(),
            )
        },
        transform = { it.toUpdatedToppingBorderVO() },
    )

    override suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = apiCaller.safeApiCallWithoutData {
        parfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
            groupId = groupId.value,
            parfaitId = parfaitId.value,
            parfaitImageId = parfaitImageId.value,
        )
    }
}
