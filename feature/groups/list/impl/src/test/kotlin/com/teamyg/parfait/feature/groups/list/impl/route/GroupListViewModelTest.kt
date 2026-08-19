package com.teamyg.parfait.feature.groups.list.impl.route

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsFlowUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshMyGroupsUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMyGroupsFlow: GetMyGroupsFlowUseCase = mockk()
    private val refreshMyGroups: RefreshMyGroupsUseCase = mockk()
    private val getMyAccountFlow: GetMyAccountFlowUseCase = mockk()

    private fun viewModel(accountFlow: Flow<MyAccountVO?> = flowOf(ACCOUNT)): GroupListViewModel {
        every { getMyAccountFlow() } returns accountFlow
        return GroupListViewModel(
            getMyGroupsFlow = getMyGroupsFlow,
            refreshMyGroups = refreshMyGroups,
            getMyAccountFlow = getMyAccountFlow,
        )
    }

    /** 화면에 서기 전에는 아무것도 조회하지 않으므로, 대부분의 테스트는 이 상태에서 시작한다 */
    private fun TestScope.enteredViewModel() = viewModel().also { viewModel ->
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()
    }

    @Test
    fun enter_cacheEmits_showsGroups() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 그룹 둘이 있고 갱신은 성공한다
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 캐시가 준 순서 그대로 들고 있다
        assertEquals(GROUPS, viewModel.state.value.groupList)
        assertFalse(viewModel.state.value.isError)
        coVerify(exactly = 1) { refreshMyGroups() }
    }

    @Test
    fun enter_refreshFailsWithEmptyCache_showsErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비었고 갱신이 실패한다
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 보여 줄 것이 없으므로 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun enter_refreshFailsWithCachedGroups_keepsList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 목록이 있는데 갱신이 실패한다
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 앞에 선다
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 낡아도 목록을 남긴다 — 뒤로 온 것만으로 화면이 사라지지 않는다
        assertFalse(viewModel.state.value.isError)
        assertEquals(GROUPS, viewModel.state.value.groupList)
    }

    @Test
    fun cacheUpdatesAfterEnter_reflectsWithoutNewRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시를 구독 중이다
        val cache = MutableStateFlow<List<MyParfaitGroupVO>?>(emptyList())
        every { getMyGroupsFlow() } returns cache
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // When 다른 화면이 그룹을 만들어 캐시가 바뀐다
        cache.value = GROUPS
        advanceUntilIdle()

        // Then 이 화면이 다시 조회하지 않고도 반영한다
        assertEquals(GROUPS, viewModel.state.value.groupList)
        coVerify(exactly = 1) { refreshMyGroups() }
    }

    @Test
    fun enter_beforeTheScreenIsShown_doesNotLoad() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹을 준다
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When ViewModel 만 만들어지고 화면은 아직 앞에 서지 않았다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 조회는 화면이 설 때 나간다 — 생성만으로 부르면 재진입 조회와 겹쳐 두 번 나간다
        coVerify(exactly = 0) { refreshMyGroups() }
    }

    @Test
    fun enter_again_requestsAnotherRefresh() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 하나를 띄워 둔 화면
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUPS.first()))
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        // When 다른 화면에 갔다가 돌아온다
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 다시 갱신을 부른다
        coVerify(exactly = 2) { refreshMyGroups() }
    }

    @Test
    fun enter_fillsTodayInTheHeader() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 그룹을 준다
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

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
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 보여줄 그룹이 없으므로 에러 화면으로 넘어간다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun enter_failsWithLoadedGroups_keepsTheList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹을 이미 띄운 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        // When 돌아오면서 나간 조회가 실패한다
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        // Then 뒤로 온 것만으로 화면이 통째로 사라지지 않도록 낡은 목록을 남긴다
        val state = viewModel.state.value
        assertEquals(GROUPS, state.groupList)
        assertFalse(state.isError)
    }

    @Test
    fun refresh_failsWithLoadedGroups_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 그룹이 있어 이미 띄운 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 사용자가 직접 당긴 새로고침이 실패한다
            coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(GroupListIntent.Refresh)
            advanceUntilIdle()

            // Then 목록은 남기되, 목록이 그대로인 것은 성공과 구분되지 않으므로 따로 알린다
            assertEquals(GroupListSideEffect.ShowRefreshError, awaitItem())
            assertEquals(GROUPS, viewModel.state.value.groupList)
        }
    }

    @Test
    fun enter_failsWithLoadedGroups_staysSilent() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 그룹이 있어 이미 띄운 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 돌아오면서 저절로 나간 조회가 실패한다
            coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(GroupListIntent.Enter)
            advanceUntilIdle()

            // Then 사용자가 시킨 일이 아니므로 토스트로 방해하지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun refresh_failsWithNoGroups_doesNotStackAToastOnTheErrorScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비어 있고 조회도 실패해 에러 화면이 뜬 상태
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))
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
        every { getMyGroupsFlow() } returns flowOf(emptyList())
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 조회는 성공했으므로 에러 화면이 아니라 빈 파르페를 띄운다
        val state = viewModel.state.value
        assertTrue(state.groupList.isNullOrEmpty())
        assertFalse(state.isError)
    }

    @Test
    fun refresh_reloadsAndClearsIndicator() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 끝난 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        // When 아래로 당겨 새로고침
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 한 번 더 조회하고 인디케이터를 내린다
        coVerify(exactly = 2) { refreshMyGroups() }
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun refresh_succeedsAfterFailure_clearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 조회가 실패해 에러 화면이 뜬 상태
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.failure(AppError.Network(cause = null))
        val viewModel = enteredViewModel()

        // When 다시 당겨 새로고침하고 이번엔 성공한다. getMyGroupsFlow() 는 이미 구독 중이라
        // 재스텁해도 캐시가 바뀌지 않으므로 다시 스텁하지 않는다 — 에러가 걷히는 것은 온전히
        // refreshMyGroups() 의 성공 자체 때문이다
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        viewModel.processIntent(GroupListIntent.Refresh)
        advanceUntilIdle()

        // Then 에러 화면이 걷힌다
        val state = viewModel.state.value
        assertFalse(state.isError)
    }

    @Test
    fun refresh_whileLoading_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 조회가 아직 끝나지 않은 화면
        val gate = CompletableDeferred<Unit>()
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = viewModel()
        viewModel.processIntent(GroupListIntent.Enter)
        runCurrent()

        // When 아래로 당겨 새로고침
        viewModel.processIntent(GroupListIntent.Refresh)
        runCurrent()

        // Then 중복 조회가 나가지 않고, 인디케이터는 도는 채로 남는다
        coVerify(exactly = 1) { refreshMyGroups() }
        assertTrue(viewModel.state.value.isRefreshing)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun clickTopping_carriesTheClickedGroup() = runTest(mainDispatcherRule.dispatcher) {
        // Given 그룹 두 개가 그려진 목록
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 두 번째 그룹의 토핑을 누른다
            viewModel.processIntent(GroupListIntent.ClickTopping(GroupId(2L)))
            advanceUntilIdle()

            // Then 첫 그룹으로 고정되지 않고 누른 그룹이 실려 간다
            assertEquals(GroupListSideEffect.NavigateToCanvas(GroupId(2L)), awaitItem())
        }
    }

    @Test
    fun init_showsTheNicknameFromTheAccountStream() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 SSoT 가 내 닉네임을 들고 있다
        every { getMyGroupsFlow() } returns flowOf(null)

        // When ViewModel 이 만들어진다 — 화면에 서기 전이다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 목록과 달리 진입을 기다리지 않고 닉네임이 채워진다
        assertEquals("모카", viewModel.state.value.nickName)
    }

    @Test
    fun accountNicknameChanges_followsTheStream() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 이미 받아 둔 화면
        every { getMyGroupsFlow() } returns flowOf(null)
        val accountFlow = MutableStateFlow<MyAccountVO?>(ACCOUNT)
        val viewModel = viewModel(accountFlow)
        advanceUntilIdle()

        // When 다른 화면에서 닉네임을 바꿔 SSoT 가 새 값을 밀어 준다
        accountFlow.value = ACCOUNT.copy(nickname = GlobalNickname("바닐라"))
        advanceUntilIdle()

        // Then 그룹 만들기로 실려 갈 값이 낡지 않도록 구독이 따라간다
        assertEquals("바닐라", viewModel.state.value.nickName)
    }

    @Test
    fun init_accountIsNotStoredYet_hasNoNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 계정이 없어 스트림이 null 을 낸다
        every { getMyGroupsFlow() } returns flowOf(null)

        // When ViewModel 이 만들어진다
        val viewModel = viewModel(accountFlow = flowOf(null))
        advanceUntilIdle()

        // Then mock 이름을 지어내지 않는다
        assertNull(viewModel.state.value.nickName)
    }

    @Test
    fun clickCreateNewGroup_carriesTheNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 받아 둔 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 그룹 만들기를 누른다
            viewModel.processIntent(GroupListIntent.ClickCreateNewGroup)
            advanceUntilIdle()

            // Then 갈 화면이 입력칸에 채울 이름을 이펙트가 들고 간다
            assertEquals(GroupListSideEffect.NavigateToCreateGroup("모카"), awaitItem())
        }
    }

    @Test
    fun clickCreateNewGroup_withoutANickname_doesNotNavigate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 계정 스트림이 아직 닉네임을 내놓지 않은 화면
        every { getMyGroupsFlow() } returns flowOf(GROUPS)
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        val viewModel = viewModel(accountFlow = flowOf(null))
        viewModel.processIntent(GroupListIntent.Enter)
        advanceUntilIdle()

        viewModel.effect.test {
            // When 그룹 만들기를 누른다
            viewModel.processIntent(GroupListIntent.ClickCreateNewGroup)
            advanceUntilIdle()

            // Then 이미 가진 이름을 다시 입력하게 두느니 열지 않는다 — 다시 누르면 열린다
            expectNoEvents()
        }
    }

    private companion object {
        val ACCOUNT = MyAccountVO(
            memberId = MemberId(1L),
            provider = LoginProvider.KAKAO,
            nickname = GlobalNickname("모카"),
        )

        val GROUPS = listOf(
            MyParfaitGroupVO(
                groupId = GroupId(1L),
                groupName = GroupName("모카의 파르페"),
                recentImageUrl = "https://cdn.example.com/a.png",
                recentImageUploadedAt = Instant.parse("2026-08-15T10:00:00Z"),
                lastPlacedByNametagChip = NametagChipType.DEFAULT,
            ),
            MyParfaitGroupVO(
                groupId = GroupId(2L),
                groupName = GroupName("우리집"),
                recentImageUrl = null,
                recentImageUploadedAt = null,
                lastPlacedByNametagChip = NametagChipType.DEFAULT,
            ),
        )
    }
}
