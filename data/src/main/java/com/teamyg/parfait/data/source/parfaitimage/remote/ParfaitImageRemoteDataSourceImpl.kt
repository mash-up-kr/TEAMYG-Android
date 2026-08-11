package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlaceRequest
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlacedToppingVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
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
}
