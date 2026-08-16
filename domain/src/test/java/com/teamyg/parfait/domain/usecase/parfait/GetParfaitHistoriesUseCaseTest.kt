package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetParfaitHistoriesUseCaseTest {
    private class FakeParfaitRepository(
        private val listResult: Result<List<PastCanvasVO>> = Result.success(emptyList()),
    ) : ParfaitRepository {
        var requestedRange: Pair<LocalDate?, LocalDate?>? = null
            private set

        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = Result.success(emptyList())

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> {
            requestedRange = from to to
            return listResult
        }

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> =
            Result.failure(IllegalStateException("쓰이지 않는다"))
    }

    private fun canvas(date: LocalDate) = PastCanvasVO(
        parfaitId = ParfaitId(date.toEpochDays()),
        date = date,
        thumbnailUrl = null,
        toppingCount = 1,
    )

    @Test
    fun invoke_asksForTheWholeYear() = runTest {
        // Given 어느 해든 응답이 오는 그룹
        val repository = FakeParfaitRepository()

        // When 2025 년 기록 조회
        GetParfaitHistoriesUseCase(repository)(GroupId(GROUP_ID), year = 2025)

        // Then 1월 1일부터 12월 31일까지 한 번에 부른다 — 월이 바뀔 때마다 다시 부르지 않는다
        assertEquals(LocalDate(2025, 1, 1) to LocalDate(2025, 12, 31), repository.requestedRange)
    }

    @Test
    fun invoke_leapYear_stillEndsOnDecemberLastDay() = runTest {
        // Given 윤년
        val repository = FakeParfaitRepository()

        // When 2024 년 기록 조회
        GetParfaitHistoriesUseCase(repository)(GroupId(GROUP_ID), year = 2024)

        // Then 2월 말일을 따로 다루지 않아도 범위가 정확하다
        assertEquals(LocalDate(2024, 1, 1) to LocalDate(2024, 12, 31), repository.requestedRange)
    }

    @Test
    fun invoke_sortsByDateDescending() = runTest {
        // Given 서버가 순서를 뒤섞어 준다
        val repository = FakeParfaitRepository(
            listResult = Result.success(
                listOf(
                    canvas(LocalDate(2025, 3, 2)),
                    canvas(LocalDate(2025, 11, 9)),
                    canvas(LocalDate(2025, 7, 21)),
                ),
            ),
        )

        // When 기록 조회
        val result = GetParfaitHistoriesUseCase(repository)(GroupId(GROUP_ID), year = 2025)

        // Then 최신순으로 정리해 준다
        assertEquals(
            listOf(LocalDate(2025, 11, 9), LocalDate(2025, 7, 21), LocalDate(2025, 3, 2)),
            result.getOrNull()?.map(PastCanvasVO::date),
        )
    }

    @Test
    fun invoke_listFails_propagatesFailure() = runTest {
        // Given 목록 조회가 실패한다
        val repository = FakeParfaitRepository(listResult = Result.failure(IOException("네트워크")))

        // When 기록 조회
        val result = GetParfaitHistoriesUseCase(repository)(GroupId(GROUP_ID), year = 2025)

        // Then 빈 목록으로 삼키지 않는다 — 호출부가 "기록 없음"과 구분해야 한다
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
    }
}
