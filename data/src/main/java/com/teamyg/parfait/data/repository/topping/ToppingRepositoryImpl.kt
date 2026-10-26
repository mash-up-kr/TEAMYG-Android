package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.topping.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class ToppingRepositoryImpl @Inject constructor(
    private val parfaitImageRemoteDataSource: ParfaitImageRemoteDataSource,
) : ToppingRepository {
    override suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> = parfaitImageRemoteDataSource
        .placeTopping(
            groupId = groupId,
            parfaitId = parfaitId,
            imageId = imageId,
            transform = transform,
            border = border,
        ).mapErrorToAppError()

    override suspend fun delete(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = parfaitImageRemoteDataSource
        .deleteTopping(groupId = groupId, parfaitId = parfaitId, parfaitImageId = parfaitImageId)
        .mapErrorToAppError()

    override suspend fun updateAll(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
        textUpdates: List<TextToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>> {
        if (updates.isEmpty() && textUpdates.isEmpty()) return Result.success(emptyList())

        return parfaitImageRemoteDataSource
            .updateToppings(groupId = groupId, parfaitId = parfaitId, updates = updates, textUpdates = textUpdates)
            .mapErrorToAppError()
    }

    override suspend fun updateBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO> = parfaitImageRemoteDataSource
        .updateToppingBorder(
            groupId = groupId,
            parfaitId = parfaitId,
            parfaitImageId = parfaitImageId,
            border = border,
        ).mapErrorToAppError()
}
