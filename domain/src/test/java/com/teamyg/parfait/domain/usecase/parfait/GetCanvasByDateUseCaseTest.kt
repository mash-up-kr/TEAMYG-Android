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
import kotlinx.datetime.minus
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetCanvasByDateUseCaseTest {
    private class FakeParfaitRepository(
        private val listResult: Result<List<PastCanvasVO>> = Result.success(emptyList()),
        private val detailResult: Result<CanvasVO> = Result.success(canvas(PARFAIT_ID)),
    ) : ParfaitRepository {
        var detailCallCount = 0
            private set
        var requestedRange: Pair<LocalDate?, LocalDate?>? = null
            private set
        var requestedParfaitId: ParfaitId? = null
            private set

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> = error("오늘 조회를 타지 않는다")

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> {
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

    private fun pastCanvas(
        id: Long,
        date: LocalDate,
    ) = PastCanvasVO(
        parfaitId = ParfaitId(id),
        date = date,
        thumbnailUrl = null,
        toppingCount = 0,
    )

    @Test
    fun invoke_canvasExistsOnThatDate_returnsItsDetail() = runTest {
        // Given 고른 날에 캔버스가 있다
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(pastCanvas(id = PARFAIT_ID, date = DATE))),
        )

        // When 그날 캔버스 조회
        val result = GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 그 id 로 상세를 받아 돌려준다
        assertEquals(ParfaitId(PARFAIT_ID), repository.requestedParfaitId)
        assertEquals(ParfaitId(PARFAIT_ID), result.getOrNull()?.parfaitId)
    }

    @Test
    fun invoke_noCanvasOnThatDate_returnsNullWithoutDetailCall() = runTest {
        // Given 그날은 아무도 캔버스를 연 적이 없다
        val repository = FakeParfaitRepository(listResult = Result.success(emptyList()))

        // When 그날 캔버스 조회
        val result = GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 빈 캔버스를 그리도록 null 이고, 상세는 부르지 않는다
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_onlyNeighbouringDateInList_returnsNull() = runTest {
        // Given 범위를 하루로 좁혔는데도 옆날 캔버스가 딸려 왔다
        val repository = FakeParfaitRepository(
            listResult = Result.success(
                listOf(pastCanvas(id = PARFAIT_ID, date = DATE.minus(DatePeriod(days = 1)))),
            ),
        )

        // When 그날 캔버스 조회
        val result = GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 날짜를 다시 보므로 옆날 것을 고른 날로 착각하지 않는다
        assertNull(result.getOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_narrowsRangeToThatDate() = runTest {
        // Given 고른 날에 캔버스가 있다
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(pastCanvas(id = PARFAIT_ID, date = DATE))),
        )

        // When 그날 캔버스 조회
        GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 30일 기본값을 받아 거르지 않고 하루로 좁혀 부른다
        assertEquals(DATE to DATE, repository.requestedRange)
    }

    @Test
    fun invoke_listFails_propagatesFailureWithoutDetailCall() = runTest {
        // Given 목록 조회가 실패한다
        val repository = FakeParfaitRepository(listResult = Result.failure(IOException("네트워크")))

        // When 그날 캔버스 조회
        val result = GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 실패를 그대로 올리고 상세는 부르지 않는다
        assertIs<IOException>(result.exceptionOrNull())
        assertEquals(0, repository.detailCallCount)
    }

    @Test
    fun invoke_detailFails_propagatesFailure() = runTest {
        // Given 목록에는 있는데 상세가 실패한다
        val repository = FakeParfaitRepository(
            listResult = Result.success(listOf(pastCanvas(id = PARFAIT_ID, date = DATE))),
            detailResult = Result.failure(IOException("네트워크")),
        )

        // When 그날 캔버스 조회
        val result = GetCanvasByDateUseCase(repository)(GroupId(GROUP_ID), DATE)

        // Then 캔버스가 없는 것(null 성공)과 구분되도록 실패로 남는다
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
        const val PARFAIT_ID = 42L
        val DATE = LocalDate(2026, 8, 3)

        fun canvas(parfaitId: Long) = CanvasVO(
            parfaitId = ParfaitId(parfaitId),
            date = DATE,
            status = CanvasStatus.CLOSED,
            lastClosedDate = null,
            members = emptyList(),
            background = null,
            toppings = emptyList(),
        )
    }
}
