package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
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
}
