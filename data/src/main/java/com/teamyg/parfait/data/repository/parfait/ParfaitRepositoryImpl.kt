package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSource
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 *
 * 오늘 캔버스는 [CanvasLocalDataSource] 인메모리 캐시가 SSoT 다
 * (`adr/0029-canvas-today-ssot-polling.md`) — 조회는 캐시를 읽는 [Flow] 하나, 서버 재조회는
 * [refreshTodayCanvas]·[refreshTodayCanvasDetail] 로 갈라 둔다.
 */
class ParfaitRepositoryImpl @Inject constructor(
    private val parfaitRemoteDataSource: ParfaitRemoteDataSource,
    private val canvasLocalDataSource: CanvasLocalDataSource,
) : ParfaitRepository {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = parfaitRemoteDataSource
        .getYears(groupId)
        .mapErrorToAppError()

    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvasLocalDataSource.todayCanvas(groupId)

    override fun cachedTodayCanvasDate(groupId: GroupId): LocalDate? =
        canvasLocalDataSource.cachedTodayCanvas(groupId)?.date

    override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> = parfaitRemoteDataSource
        .getTodayCanvas(groupId)
        .onSuccess { canvas -> canvasLocalDataSource.saveTodayCanvas(groupId, canvas) }
        .map { }
        .mapErrorToAppError()

    override suspend fun refreshTodayCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = parfaitRemoteDataSource
        .getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
        .onSuccess { canvas -> canvasLocalDataSource.saveTodayCanvas(groupId, canvas) }
        .map { }
        .mapErrorToAppError()

    override fun clearTodayCanvas() = canvasLocalDataSource.clear()

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

    override suspend fun changeCanvasBackground(
        groupId: GroupId,
        parfaitId: ParfaitId,
        background: CanvasBackgroundEdit,
    ): Result<CanvasBackground?> = parfaitRemoteDataSource
        .changeCanvasBackground(groupId = groupId, parfaitId = parfaitId, background = background)
        .mapErrorToAppError()
}
