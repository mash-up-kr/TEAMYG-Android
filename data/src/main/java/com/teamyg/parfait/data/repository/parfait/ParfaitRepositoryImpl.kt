package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
class ParfaitRepositoryImpl @Inject constructor(
    private val parfaitRemoteDataSource: ParfaitRemoteDataSource,
) : ParfaitRepository {
    override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> = parfaitRemoteDataSource
        .getTodayCanvas(groupId)
        .mapErrorToAppError()

    override suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate?,
        to: LocalDate?,
    ): Result<List<PastCanvasVO>> = parfaitRemoteDataSource
        .getPastCanvases(groupId = groupId, from = from, to = to)
        .mapErrorToAppError()

    override suspend fun getCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO> = parfaitRemoteDataSource
        .getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
        .mapErrorToAppError()
}
