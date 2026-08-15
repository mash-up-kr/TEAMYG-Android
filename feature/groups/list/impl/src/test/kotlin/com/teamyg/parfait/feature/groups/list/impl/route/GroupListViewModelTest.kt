package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyGroups: GetMyGroupsUseCase = mockk()

    private fun viewModel() = GroupListViewModel(getMyGroups)

    @Test
    fun init_loadSucceeds_showsGroupsInServerOrder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹 두 개를 준다
        coEvery { getMyGroups() } returns Result.success(GROUPS)

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 서버가 준 순서 그대로 들고 있고 에러 화면은 뜨지 않는다
        val state = viewModel.state.value
        assertEquals(GROUPS, state.groupList)
        assertFalse(state.isError)
        coVerify(exactly = 1) { getMyGroups() }
    }

    @Test
    fun init_loadFails_showsErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 보여줄 그룹이 없으므로 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun init_loadSucceedsWithNoGroups_isNotError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입한 그룹이 없어 빈 배열이 온다
        coEvery { getMyGroups() } returns Result.success(emptyList())

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 조회는 성공했으므로 에러 화면이 아니라 빈 파르페를 띄운다
        val state = viewModel.state.value
        assertTrue(state.groupList.isEmpty())
        assertFalse(state.isError)
    }

    @Test
    fun refresh_reloadsAndClearsIndicator() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 끝난 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 아래로 당겨 새로고침
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 한 번 더 조회하고 인디케이터를 내린다
        coVerify(exactly = 2) { getMyGroups() }
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun refresh_fails_showsErrorScreenEvenWithLoadedGroups() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹을 이미 띄운 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 새로고침이 실패한다
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 실패를 알릴 다른 자리가 없으므로 목록이 있어도 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun refresh_succeedsAfterFailure_returnsToTheList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 에러 화면이 뜬 상태
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 다시 당겨 새로고침하고 이번엔 성공한다
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 에러 화면이 걷히고 목록이 돌아온다
        val state = viewModel.state.value
        assertEquals(GROUPS, state.groupList)
        assertFalse(state.isError)
    }

    @Test
    fun refresh_whileLoading_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        coEvery { getMyGroups() } coAnswers {
            gate.await()
            Result.success(GROUPS)
        }
        val viewModel = viewModel()
        runCurrent()

        // When 아래로 당겨 새로고침
        viewModel.processIntent(GroupListIntent.Refresh)
        runCurrent()

        // Then 중복 조회가 나가지 않고, 인디케이터는 도는 채로 남는다
        coVerify(exactly = 1) { getMyGroups() }
        assertTrue(viewModel.state.value.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
    }

    private companion object {
        val GROUPS = listOf(
            MyParfaitGroupVO(
                groupId = GroupId(1L),
                groupName = GroupName("모카의 파르페"),
                recentImageUrl = "https://cdn.example.com/a.png",
                recentImageUploadedAt = LocalDateTime(2026, 8, 15, 10, 0),
            ),
            MyParfaitGroupVO(
                groupId = GroupId(2L),
                groupName = GroupName("우리집"),
                recentImageUrl = null,
                recentImageUploadedAt = null,
            ),
        )
    }
}
