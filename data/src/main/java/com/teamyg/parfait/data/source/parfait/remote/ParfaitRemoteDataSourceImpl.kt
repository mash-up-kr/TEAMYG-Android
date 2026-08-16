package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.source.parfait.mapper.toCanvasBackground
import com.teamyg.parfait.data.source.parfait.mapper.toCanvasVO
import com.teamyg.parfait.data.source.parfait.mapper.toPastCanvasVOList
import com.teamyg.parfait.data.source.parfait.mapper.toRequest
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class ParfaitRemoteDataSourceImpl @Inject constructor(
    private val parfaitService: ParfaitService,
    private val apiCaller: ApiCaller,
) : ParfaitRemoteDataSource {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = apiCaller
        .safeApiCall(
            block = { parfaitService.getGroupsByGroupIdParfaitsYear(groupId.value) },
            transform = { it.years },
        )

    override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> = apiCaller.safeApiCall(
        block = { parfaitService.getGroupsByGroupIdParfaitsToday(groupId.value) },
        transform = { it.toCanvasVO() },
    )

    override suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate?,
        to: LocalDate?,
    ): Result<List<PastCanvasVO>> = apiCaller.safeApiCall(
        block = {
            parfaitService.getGroupsByGroupIdParfaits(
                groupId = groupId.value,
                from = from?.toString(),
                to = to?.toString(),
            )
        },
        transform = { it.toPastCanvasVOList() },
    )

    override suspend fun getCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO> = apiCaller.safeApiCall(
        block = {
            parfaitService.getGroupsByGroupIdParfaitsByParfaitId(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
            )
        },
        transform = { it.toCanvasVO() },
    )

    override suspend fun changeCanvasBackground(
        groupId: GroupId,
        parfaitId: ParfaitId,
        background: CanvasBackgroundEdit,
    ): Result<CanvasBackground?> = apiCaller.safeApiCall(
        block = {
            parfaitService.patchGroupsByGroupIdParfaitsByParfaitIdBackground(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                request = background.toRequest(),
            )
        },
        transform = { it.toCanvasBackground() },
    )
}
