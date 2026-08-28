package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetParfaitYearsUseCaseTest {
    private class FakeParfaitRepository(
        private val yearsResult: Result<List<Int>>,
    ) : ParfaitRepository {
        override suspend fun getYears(groupId: GroupId): Result<List<Int>> = yearsResult

        override suspend fun getPastCanvases(
            groupId: GroupId,
            from: LocalDate?,
            to: LocalDate?,
        ): Result<List<PastCanvasVO>> = Result.success(emptyList())

        override fun todayCanvas(groupId: GroupId): Flow<CanvasVO?> = error("연도 조회는 오늘 캔버스를 보지 않는다")

        override suspend fun refreshTodayCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<Unit> = error("연도 조회는 오늘 캔버스를 갱신하지 않는다")

        override fun clearTodayCanvas() = error("연도 조회는 캐시를 지우지 않는다")

        override fun requestTodayCanvasRefresh(groupId: GroupId) = error("연도 조회는 갱신을 요청하지 않는다")

        override suspend fun getCanvasDetail(
            groupId: GroupId,
            parfaitId: ParfaitId,
        ): Result<CanvasVO> = Result.failure(IllegalStateException("쓰이지 않는다"))

        override suspend fun changeCanvasBackground(
            groupId: GroupId,
            parfaitId: ParfaitId,
            background: CanvasBackgroundEdit,
        ): Result<CanvasBackground?> = Result.failure(IllegalStateException("쓰이지 않는다"))
    }

    private val thisYear = parfaitToday().year

    @Test
    fun invoke_serverOmitsThisYear_addsIt() = runTest {
        // Given 올해는 아직 파르페가 하나도 없어 서버가 빼고 준다
        val repository = FakeParfaitRepository(Result.success(listOf(thisYear - 1, thisYear - 2)))

        // When 연도 조회
        val result = GetParfaitYearsUseCase(repository)(GroupId(GROUP_ID))

        // Then 오늘로 돌아올 수 있도록 올해를 채워 최신순으로 준다
        assertEquals(listOf(thisYear, thisYear - 1, thisYear - 2), result.getOrNull())
    }

    @Test
    fun invoke_serverIncludesThisYear_keepsListAsIs() = runTest {
        // Given 올해도 파르페가 있어 서버가 이미 넣어 준다
        val years = listOf(thisYear, thisYear - 1)
        val repository = FakeParfaitRepository(Result.success(years))

        // When 연도 조회
        val result = GetParfaitYearsUseCase(repository)(GroupId(GROUP_ID))

        // Then 중복해 넣지 않는다
        assertEquals(years, result.getOrNull())
    }

    @Test
    fun invoke_serverReturnsNothing_stillOffersThisYear() = runTest {
        // Given 그룹에 파르페가 하나도 없다
        val repository = FakeParfaitRepository(Result.success(emptyList()))

        // When 연도 조회
        val result = GetParfaitYearsUseCase(repository)(GroupId(GROUP_ID))

        // Then 드롭다운이 비어 달력이 잠기지 않도록 올해만은 남는다
        assertEquals(listOf(thisYear), result.getOrNull())
    }

    @Test
    fun invoke_fails_propagatesFailure() = runTest {
        // Given 연도 조회가 실패한다
        val repository = FakeParfaitRepository(Result.failure(IOException("네트워크")))

        // When 연도 조회
        val result = GetParfaitYearsUseCase(repository)(GroupId(GROUP_ID))

        // Then 올해만 든 목록으로 성공을 꾸미지 않는다
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
    }
}
