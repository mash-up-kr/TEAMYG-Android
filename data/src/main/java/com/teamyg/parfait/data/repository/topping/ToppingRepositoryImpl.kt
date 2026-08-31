package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
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
    ): Result<List<UpdatedToppingVO>> {
        // 서버가 빈 items 를 200 으로 받아 주지만 보낼 이유가 없다(`api/parfait-image.md`)
        if (updates.isEmpty()) return Result.success(emptyList())

        return parfaitImageRemoteDataSource
            .updateToppings(groupId = groupId, parfaitId = parfaitId, updates = updates)
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
