package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

class GetTodayParfaitUseCaseTest {
    private class FakeParfaitRepository(
        private val listResult: Result<List<PastCanvasVO>> = Result.success(emptyList()),
        private val detailResult: Result<CanvasVO> = Result.success(canvas(PARFAIT_ID)),
    ) : ParfaitRepository {
        var listCallCount = 0
            private set
        var detailCallCount = 0
            private set
        var requestedRange: Pair<LocalDate?, LocalDate?>? = null
            private set
        var requestedParfaitId: ParfaitId? = null
            private set

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> {
            listCallCount++
            requestedRange = from to to
            return listResult
        }

        override suspend fun getCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<CanvasVO> {
            detailCallCount++
            requestedParfaitId = parfaitId
            return detailResult
        }
    }

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun history(
        id: Long,
        date: LocalDate,
    ) = PastCanvasVO(
        parfaitId = ParfaitId(id),
        date = date,
        thumbnailUrl = null,
        toppingCount = 0,
    )

    @Test
    fun invoke_todayCanvasExists_returnsDetailOfThatCanvas() = runTest {
        // Given 오늘 날짜 캔버스가 목록에 있다
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(history(id = PARFAIT_ID, date = today))),
        )

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 그 id 로 상세를 받아 돌려준다
        assertEquals(ParfaitId(PARFAIT_ID), repository.requestedParfaitId)
        assertEquals(ParfaitId(PARFAIT_ID), result.getOrNull()?.parfaitId)
    }

    @Test
    fun invoke_todayCanvasMissing_returnsNullWithoutDetailCall() = runTest {
        // Given 목록이 비어 있다 — 아직 아무도 오늘 캔버스를 열지 않았다
        val repository = FakeParfaitRepository(listResult = Result.success(emptyList()))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 상세를 부르지 않고 null 이다 — 여기서 캔버스를 만들지 않는다
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_onlyYesterdayInList_returnsNull() = runTest {
        // Given 범위를 오늘로 좁혔는데도 어제 캔버스가 딸려 왔다
        val yesterday = today.minus(DatePeriod(days = 1))
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(history(id = PARFAIT_ID, date = yesterday))),
        )

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 날짜를 다시 보므로 어제 것을 오늘로 착각하지 않는다
        assertNull(result.getOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_narrowsRangeToToday() = runTest {
        // Given 오늘 캔버스가 있는 그룹
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(history(id = PARFAIT_ID, date = today))),
        )

        // When 오늘 파르페 조회
        GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 30일 기본값을 받아 거르지 않고 하루로 좁혀 부른다
        assertEquals(today to today, repository.requestedRange)
        assertEquals(1, repository.listCallCount)
    }

    @Test
    fun invoke_listFails_propagatesFailureWithoutDetailCall() = runTest {
        // Given 목록 조회가 실패한다
        val repository = FakeParfaitRepository(listResult = Result.failure(IOException("네트워크")))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 실패를 그대로 올리고 상세는 부르지 않는다
        assertIs<IOException>(result.exceptionOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_detailFails_propagatesFailure() = runTest {
        // Given 목록에는 있는데 상세가 실패한다
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(history(id = PARFAIT_ID, date = today))),
            detailResult = Result.failure(IOException("네트워크")),
        )

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 캔버스가 없는 것(null 성공)과 구분되도록 실패로 남는다
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
        const val PARFAIT_ID = 42L

        fun canvas(parfaitId: Long) = CanvasVO(
            parfaitId = ParfaitId(parfaitId),
            date = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            status = CanvasStatus.ACTIVE,
            lastClosedDate = null,
            members = emptyList(),
            background = null,
            toppings = emptyList(),
        )
    }
}
