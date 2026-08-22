package com.teamyg.parfait.data.repository.parfaitimage

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.repository.parfaitimage.ParfaitImageRepository
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
class ParfaitImageRepositoryImpl @Inject constructor(
    private val parfaitImageRemoteDataSource: ParfaitImageRemoteDataSource,
) : ParfaitImageRepository {
    override suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = parfaitImageRemoteDataSource
        .deleteTopping(groupId = groupId, parfaitId = parfaitId, parfaitImageId = parfaitImageId)
        .mapErrorToAppError()
}
