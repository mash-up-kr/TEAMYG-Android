package com.teamyg.parfait.feature.groups.list.impl.route

import app.cash.turbine.test
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyGroups: GetMyGroupsUseCase = mockk()

    private fun viewModel() = GroupListViewModel(getMyGroups)

    /** 화면에 서기 전에는 아무것도 조회하지 않으므로, 대부분의 테스트는 이 상태에서 시작한다 */
    private fun TestScope.enteredViewModel() = viewModel().also { viewModel ->
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()
    }

    @Test
    fun enter_loadSucceeds_showsGroupsInServerOrder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹 두 개를 준다
        coEvery { getMyGroups() } returns Result.success(GROUPS)

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 서버가 준 순서 그대로 들고 있고 에러 화면은 뜨지 않는다
        val state = viewModel.state.value
        assertEquals(GROUPS, state.groupList)
        assertFalse(state.isError)
        coVerify(exactly = 1) { getMyGroups() }
    }

    @Test
    fun enter_beforeTheScreenIsShown_doesNotLoad() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹을 준다
        coEvery { getMyGroups() } returns Result.success(GROUPS)

        // When ViewModel 만 만들어지고 화면은 아직 앞에 서지 않았다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 조회는 화면이 설 때 나간다 — 생성만으로 부르면 재진입 조회와 겹쳐 두 번 나간다
        coVerify(exactly = 0) { getMyGroups() }
        val state = viewModel.state.value
        assertTrue(state.groupList.isEmpty())
    }

    @Test
    fun enter_again_reloadsTheList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 하나를 띄워 둔 화면
        coEvery { getMyGroups() } returns Result.success(listOf(GROUPS.first()))
        val viewModel = enteredViewModel()

        // When 다른 화면에 갔다가 돌아온 사이 그룹이 하나 늘었다
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 늘어난 목록이 보인다
        assertEquals(GROUPS, viewModel.state.value.groupList)
        coVerify(exactly = 2) { getMyGroups() }
    }

    @Test
    fun enter_fillsTodayInTheHeader() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹을 준다
        coEvery { getMyGroups() } returns Result.success(GROUPS)

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 헤더의 날짜가 채워진다 — 자정을 넘겨 돌아와도 다시 세도록 진입에 묶여 있다
        val state = viewModel.state.value
        assertTrue(state.dateString.isNotEmpty())
        assertTrue(state.dayOfWeekString.isNotEmpty())
    }

    @Test
    fun enter_loadFails_showsErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연결 실패
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 보여줄 그룹이 없으므로 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun enter_failsWithLoadedGroups_keepsTheList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹을 이미 띄운 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = enteredViewModel()

        // When 돌아오면서 나간 조회가 실패한다
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 뒤로 온 것만으로 화면이 통째로 사라지지 않도록 낡은 목록을 남긴다
        val state = viewModel.state.value
        assertEquals(GROUPS, state.groupList)
        assertFalse(state.isError)
    }

    @Test
    fun refresh_failsWithLoadedGroups_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹을 이미 띄운 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 사용자가 직접 당긴 새로고침이 실패한다
            coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(GroupListIntent.Refresh)
            advanceUntilIdle()

            // Then 목록은 남기되, 목록이 그대로인 것은 성공과 구분되지 않으므로 따로 알린다
            assertEquals(GroupListSideEffect.ShowRefreshError, awaitItem())
            assertEquals(GROUPS, viewModel.state.value.groupList)
        }
    }

    @Test
    fun enter_failsWithLoadedGroups_staysSilent() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹을 이미 띄운 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 돌아오면서 저절로 나간 조회가 실패한다
            coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(GroupListIntent.Enter)
            advanceUntilIdle()

            // Then 사용자가 시킨 일이 아니므로 토스트로 방해하지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun refresh_failsWithNoGroups_doesNotStackAToastOnTheErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 에러 화면이 뜬 상태
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 에러 화면에서 당겨 새로고침했는데 또 실패한다
            viewModel.processIntent(GroupListIntent.Refresh)
            advanceUntilIdle()

            // Then 에러 화면이 이미 실패를 말하고 있어 토스트를 겹치지 않는다
            assertTrue(viewModel.state.value.isError)
            expectNoEvents()
        }
    }

    @Test
    fun enter_loadSucceedsWithNoGroups_isNotError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 가입한 그룹이 없어 빈 배열이 온다
        coEvery { getMyGroups() } returns Result.success(emptyList())

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 조회는 성공했으므로 에러 화면이 아니라 빈 파르페를 띄운다
        val state = viewModel.state.value
        assertTrue(state.groupList.isEmpty())
        assertFalse(state.isError)
    }

    @Test
    fun refresh_reloadsAndClearsIndicator() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 끝난 화면
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = enteredViewModel()

        // When 아래로 당겨 새로고침
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 한 번 더 조회하고 인디케이터를 내린다
        coVerify(exactly = 2) { getMyGroups() }
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun refresh_succeedsAfterFailure_returnsToTheList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 에러 화면이 뜬 상태
        coEvery { getMyGroups() } returns Result.failure(AppError.Network(cause = null))
        val viewModel = enteredViewModel()

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
        viewModel.processIntent(GroupListIntent.Enter)
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

    @Test
    fun clickTopping_carriesTheClickedGroup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 두 개가 그려진 목록
        coEvery { getMyGroups() } returns Result.success(GROUPS)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 두 번째 그룹의 토핑을 누른다
            viewModel.processIntent(GroupListIntent.ClickTopping(GroupId(2L)))
            advanceUntilIdle()

            // Then 첫 그룹으로 고정되지 않고 누른 그룹이 실려 간다
            assertEquals(GroupListSideEffect.NavigateToCanvas(GroupId(2L)), awaitItem())
        }
    }

    private companion object {
        val GROUPS = listOf(
            MyParfaitGroupVO(
                groupId = GroupId(1L),
                groupName = GroupName("모카의 파르페"),
                recentImageUrl = "https://cdn.example.com/a.png",
                recentImageUploadedAt = Instant.parse("2026-08-15T10:00:00Z"),
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
