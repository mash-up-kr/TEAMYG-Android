package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
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
import kotlin.test.assertTrue
import kotlin.time.Clock

class GetTodayParfaitUseCaseTest {
    /**
     * 호출할 때마다 [results] 를 순서대로 돌려준다 — 재요청이 앞선 응답을 그대로 다시 받지
     * 않아야 검증이 된다. 목록이 마르면 마지막 값을 계속 준다.
     */
    private class FakeParfaitRepository(
        private val results: List<Result<CanvasVO>>,
    ) : ParfaitRepository {
        var callCount = 0
            private set

        override suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO> {
            val result = results[minOf(callCount, results.lastIndex)]
            callCount++
            return result
        }
    }

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private val yesterday = today.minus(DatePeriod(days = 1))

    @Test
    fun invoke_canvasIsDatedToday_returnsItWithoutAskingAgain() = runTest {
        // Given 서버가 오늘 날짜 캔버스를 준다
        val repository = FakeParfaitRepository(listOf(Result.success(canvas(PARFAIT_ID, today))))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 그대로 돌려주고 한 번만 부른다 — 부작용 있는 호출을 아낀다
        assertEquals(ParfaitId(PARFAIT_ID), result.getOrNull()?.parfaitId)
        assertEquals(1, repository.callCount)
    }

    @Test
    fun invoke_canvasIsDatedYesterday_asksAgainAndTakesTheSecond() = runTest {
        // Given 자정을 넘기며 어제 캔버스를 받았고, 다시 부르면 오늘 것이 온다
        val repository = FakeParfaitRepository(
            listOf(
                Result.success(canvas(STALE_PARFAIT_ID, yesterday)),
                Result.success(canvas(PARFAIT_ID, today)),
            ),
        )

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 두 번째 응답을 쓴다
        assertEquals(2, repository.callCount)
        assertEquals(ParfaitId(PARFAIT_ID), result.getOrNull()?.parfaitId)
    }

    @Test
    fun invoke_stillStaleOnRetry_stopsAtTwoCallsAndUsesIt() = runTest {
        // Given 다시 불러도 어제 캔버스다 — 기기와 서버의 날짜 기준이 다르다
        val repository = FakeParfaitRepository(listOf(Result.success(canvas(STALE_PARFAIT_ID, yesterday))))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 더 부르지 않고 받은 것을 쓴다 — 계속 불러도 같은 답이 온다
        assertEquals(2, repository.callCount)
        assertEquals(ParfaitId(STALE_PARFAIT_ID), result.getOrNull()?.parfaitId)
    }

    @Test
    fun invoke_firstCallFails_propagatesFailureWithoutRetry() = runTest {
        // Given 조회가 실패한다
        val repository = FakeParfaitRepository(listOf(Result.failure(IOException("네트워크"))))

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 실패를 그대로 올린다 — 재요청은 날짜가 어긋났을 때만이지 실패 복구가 아니다
        assertTrue(result.isFailure)
        assertIs<IOException>(result.exceptionOrNull())
        assertEquals(1, repository.callCount)
    }

    @Test
    fun invoke_retryFails_propagatesFailure() = runTest {
        // Given 어제 캔버스를 받아 다시 불렀는데 그 요청이 실패한다
        val repository = FakeParfaitRepository(
            listOf(
                Result.success(canvas(STALE_PARFAIT_ID, yesterday)),
                Result.failure(IOException("네트워크")),
            ),
        )

        // When 오늘 파르페 조회
        val result = GetTodayParfaitUseCase(repository)(GroupId(GROUP_ID))

        // Then 어제 캔버스로 눙치지 않고 실패로 남긴다
        assertIs<IOException>(result.exceptionOrNull())
    }

    private companion object {
        const val GROUP_ID = 7L
        const val PARFAIT_ID = 42L
        const val STALE_PARFAIT_ID = 41L

        fun canvas(
            parfaitId: Long,
            date: LocalDate,
        ) = CanvasVO(
            parfaitId = ParfaitId(parfaitId),
            date = date,
            status = CanvasStatus.ACTIVE,
            lastClosedDate = null,
            members = emptyList(),
            background = null,
            toppings = emptyList(),
        )
    }
}
