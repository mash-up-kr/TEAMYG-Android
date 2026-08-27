package com.teamyg.parfait.domain.usecase.parfait

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val GROUP = GroupId(1L)

class GetTodayParfaitFlowUseCaseTest {
    private class FakeParfaitRepository(
        private val canvas: Flow<CanvasVO?>,
    ) : ParfaitRepository {
        override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = canvas

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = error("구독 유스케이스는 연도를 보지 않는다")

        override suspend fun refreshTodayCanvas(groupId: GroupId): Result<Unit> = error("구독 유스케이스는 갱신하지 않는다")

        override suspend fun refreshTodayCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<Unit> = error("구독 유스케이스는 상세를 갱신하지 않는다")

        override fun cachedTodayCanvasDate(groupId: GroupId): LocalDate? = error("구독 유스케이스는 peek 하지 않는다")

        override fun clearTodayCanvas() = error("구독 유스케이스는 캐시를 지우지 않는다")

        override fun requestTodayCanvasRefresh(groupId: GroupId) = error("구독 유스케이스는 갱신을 요청하지 않는다")

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> = error("구독 유스케이스는 목록을 보지 않는다")

        override suspend fun getCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<CanvasVO> = error("구독 유스케이스는 상세를 따로 부르지 않는다")

        override suspend fun changeCanvasBackground(
            groupId: GroupId,
            parfaitId: ParfaitId,
            background: CanvasBackgroundEdit,
        ): Result<CanvasBackground?> = error("구독 유스케이스는 배경을 바꾸지 않는다")
    }

    private fun canvas(date: LocalDate) = CanvasVO(
        parfaitId = ParfaitId(100L),
        date = date,
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = emptyList(),
    )

    private fun useCaseWith(canvas: CanvasVO?) = GetTodayParfaitFlowUseCase(FakeParfaitRepository(flowOf(canvas)))

    @Test
    fun invoke_todaysCanvas_passesThrough() = runTest {
        useCaseWith(canvas(parfaitToday())).invoke(GROUP).test {
            assertEquals(ParfaitId(100L), awaitItem()?.parfaitId)
            awaitComplete()
        }
    }

    @Test
    fun invoke_yesterdaysCanvas_isFilteredToNull() = runTest {
        val yesterday = parfaitToday().minus(DatePeriod(days = 1))

        useCaseWith(canvas(yesterday)).invoke(GROUP).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_notFetchedYet_staysNull() = runTest {
        useCaseWith(null).invoke(GROUP).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
