package com.teamyg.parfait.data.repository.parfait

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasPoller
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 *
 * 오늘 캔버스는 [CanvasLocalDataSource] 인메모리 캐시가 SSoT 다
 * (`adr/0029-canvas-today-ssot-polling.md`) — 조회는 캐시를 읽는 [Flow] 하나, 서버 재조회는
 * [refreshTodayCanvas]·[refreshTodayCanvasDetail] 로 갈라 둔다. 주기 재조회는 [CanvasPoller] 가
 * 맡는다.
 */
class ParfaitRepositoryImpl @Inject constructor(
    private val parfaitRemoteDataSource: ParfaitRemoteDataSource,
    private val canvasLocalDataSource: CanvasLocalDataSource,
    private val canvasPoller: CanvasPoller,
) : ParfaitRepository {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = parfaitRemoteDataSource
        .getYears(groupId)
        .mapErrorToAppError()

    /**
     * 구독이 붙어 있는 동안만 폴러가 돈다 — 화면이 보지 않는 캔버스를 계속 부르지 않는다.
     * 화면은 폴러의 존재를 모른다.
     */
    override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvasLocalDataSource
        .todayCanvas(groupId)
        .onStart { canvasPoller.acquire(groupId) }
        .onCompletion { canvasPoller.release(groupId) }

    override fun cachedTodayCanvasDate(groupId: GroupId): LocalDate? =
        canvasLocalDataSource.cachedTodayCanvas(groupId)?.date

    /** 폴러를 지나므로 이 갱신도 주기를 다시 세운다 */
    override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> = canvasPoller
        .refreshNow(groupId, forceToday = true)
        .mapErrorToAppError()

    override suspend fun refreshTodayCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = canvasPoller
        .refreshNow(groupId)
        .mapErrorToAppError()

    override fun clearTodayCanvas() {
        canvasPoller.stopAll()
        canvasLocalDataSource.clear()
    }

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
