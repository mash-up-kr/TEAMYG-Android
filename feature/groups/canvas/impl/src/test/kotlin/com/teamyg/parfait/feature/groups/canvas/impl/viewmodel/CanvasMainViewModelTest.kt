package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsFlowUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshMyGroupsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitHistoriesUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitYearsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CanvasMainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getParfaitHistories: GetParfaitHistoriesUseCase = mockk()
    private val getParfaitYears: GetParfaitYearsUseCase = mockk()
    private val getTodayParfait: GetTodayParfaitUseCase = mockk()
    private val getParfaitDetail: GetParfaitDetailUseCase = mockk()
    private val getMyGroupsFlow: GetMyGroupsFlowUseCase = mockk()
    private val refreshMyGroups: RefreshMyGroupsUseCase = mockk()
    private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxUnitFun = true)

    private val today = parfaitToday()

    private val yesterday = today.minus(DatePeriod(days = 1))

    private val tomorrow = today.plus(DatePeriod(days = 1))

    /**
     * 달력은 [GetParfaitHistoriesUseCase] 가 준 목록에서만 날짜를 열어 준다. 지난 날을 보는
     * 상태를 만들려면 그 목록에 어제가 들어 있어야 한다.
     */
    @Before
    fun stubTheHappyPath() {
        coEvery { getParfaitYears(any()) } returns Result.success(listOf(today.year))
        coEvery { getParfaitHistories(any(), any()) } returns Result.success(
            listOf(
                PastCanvasVO(
                    parfaitId = ParfaitId(YESTERDAY_PARFAIT_ID),
                    date = yesterday,
                    thumbnailUrl = null,
                    toppingCount = 1,
                ),
            ),
        )
        coEvery { getTodayParfait(any()) } returns Result.success(canvas(TODAY_PARFAIT_ID, today))
        coEvery { getParfaitDetail(any(), any()) } returns Result.success(canvas(YESTERDAY_PARFAIT_ID, yesterday))
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUP))
        coEvery { refreshMyGroups() } returns Result.success(Unit)
    }

    /** [mockkStatic] 은 JVM 전역 상태라 다음 테스트로 새지 않도록 매번 걷어 낸다 */
    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun viewModel() = CanvasMainViewModel(
        groupIdValue = GROUP_ID,
        getParfaitHistoriesUseCase = getParfaitHistories,
        getParfaitYearsUseCase = getParfaitYears,
        getTodayParfaitUseCase = getTodayParfait,
        getParfaitDetailUseCase = getParfaitDetail,
        getMyGroupsFlowUseCase = getMyGroupsFlow,
        refreshMyGroupsUseCase = refreshMyGroups,
        toppingDraftRepository = toppingDraftRepository,
    )

    /** 화면에 서기 전에는 캔버스를 부르지 않으므로, 대부분의 테스트는 이 상태에서 시작한다 */
    private fun TestScope.enteredViewModel() = viewModel().also { viewModel ->
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()
    }

    @Test
    fun enter_loadsTodayCanvasAndThisYearsHistories() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 오늘 캔버스를 그리고, 달력이 점을 찍을 올해 기록도 받아 둔다
        val state = viewModel.state.value
        assertEquals(ParfaitId(TODAY_PARFAIT_ID), state.viewedCanvas?.parfaitId)
        assertEquals(setOf(yesterday), state.uploadedDates)
        coVerify(exactly = 1) { getParfaitHistories(any(), today.year) }
    }

    @Test
    fun enter_beforeTheScreenIsShown_doesNotLoadTheCanvas() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When ViewModel 만 만들어지고 화면은 아직 앞에 서지 않았다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 오늘 조회는 캔버스를 만들어 저장하므로 화면이 설 때까지 미룬다
        coVerify(exactly = 0) { getTodayParfait(any()) }
        val state = viewModel.state.value
        assertTrue(state.viewedCanvas == null)
    }

    @Test
    fun enter_again_reloadsTheTodayCanvas() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 띄운 화면
        val viewModel = enteredViewModel()

        // When 자리를 비운 사이 새 멤버가 그룹에 들어왔고, 화면으로 돌아온다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today, members = listOf(member(NEW_MEMBER_NICKNAME))),
        )
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        // Then 새 멤버의 네임태그가 보인다
        val chips = viewModel.state.value.memberChips
        assertEquals(listOf(NEW_MEMBER_NICKNAME), chips.map(GroupMemberChip::nickname))
        coVerify(exactly = 2) { getTodayParfait(any()) }
    }

    @Test
    fun enter_whileViewingAPastDate_keepsThatDayAsIs() = runTest(mainDispatcherRule.dispatcher) {
        // Given 달력에서 어제로 옮겨 둔 화면
        val viewModel = enteredViewModel()
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        // When 다른 화면에 갔다가 돌아온다
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        // Then 마감된 날은 더 바뀌지 않으므로 다시 묻지 않고, 보던 날도 그대로다
        val state = viewModel.state.value
        assertEquals(yesterday, state.selectedDate)
        assertEquals(ParfaitId(YESTERDAY_PARFAIT_ID), state.viewedCanvas?.parfaitId)
        coVerify(exactly = 1) { getTodayParfait(any()) }
        coVerify(exactly = 1) { getParfaitHistories(any(), any()) }
    }

    @Test
    fun enter_doesNotAskForTheYearsAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 한 번 열린 상태
        val viewModel = enteredViewModel()

        // When 다시 돌아온다
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        // Then 연도 목록은 재진입마다 물어볼 값이 아니라 init 에 남아 있다
        coVerify(exactly = 1) { getParfaitYears(any()) }
    }

    @Test
    fun init_cacheHasGroup_showsGroupName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록 캐시에 이 그룹이 있다
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUP))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 캐시의 이름이 화면에 온다
        assertEquals("아메리카노", viewModel.state.value.groupName)
        coVerify(exactly = 0) { refreshMyGroups() }
    }

    @Test
    fun init_cacheEmpty_refreshesListOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시가 비어 있다(프로세스 재시작 후 캔버스로 복귀)
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 목록을 한 번 받아 온다. 실패해도 캔버스는 계속 그린다
        coVerify(exactly = 1) { refreshMyGroups() }
    }

    @Test
    fun enter_memberChips_followTheServerAssignedChip() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 두 멤버에게 12종 중 서로 다른 값을 배정했다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(
                TODAY_PARFAIT_ID,
                today,
                members = listOf(
                    member("모카", NametagChipType.TYPE7),
                    member("판다", NametagChipType.TYPE2),
                ),
            ),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 목록 순서가 아니라 배정된 값이 색을 정한다
        assertEquals(
            listOf(YGColorChipType.NametagChip7, YGColorChipType.NametagChip2),
            viewModel.state.value.memberChips
                .map(GroupMemberChip::colorChipType),
        )
    }

    @Test
    fun enter_memberChips_doNotShiftWhenAnEarlierMemberLeaves() = runTest(mainDispatcherRule.dispatcher) {
        // Given 앞자리 멤버가 빠지고 뒤의 멤버만 남았다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today, members = listOf(member("판다", NametagChipType.TYPE2))),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 남은 사람 색이 밀리지 않는다 — 인덱스 규칙이었다면 첫 칸 색이 됐다
        assertEquals(
            listOf(YGColorChipType.NametagChip2),
            viewModel.state.value.memberChips
                .map(GroupMemberChip::colorChipType),
        )
    }

    @Test
    fun enter_memberWithDefaultChip_getsTheNeutralColour() = runTest(mainDispatcherRule.dispatcher) {
        // Given 서버가 앱이 모르는 값을 줘 매퍼가 DEFAULT 로 접었다
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(
                TODAY_PARFAIT_ID,
                today,
                members = listOf(member("모카", nametagChip = NametagChipType.DEFAULT)),
            ),
        )

        // When 화면에 들어간다
        val viewModel = enteredViewModel()

        // Then 아무 색이나 돌리지 않는다
        assertEquals(
            listOf(YGColorChipType.Default),
            viewModel.state.value.memberChips
                .map(GroupMemberChip::colorChipType),
        )
    }

    @Test
    fun enter_todayCanvasFailsWithNothingOnScreen_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 한 번도 못 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 화면이 앞에 서면서 나간 조회가 실패한다
            viewModel.processIntent(CanvasMainIntent.Enter)
            advanceUntilIdle()

            // Then 빈 캔버스와 조회 실패가 구분되지 않으므로 따로 알린다. 알리지 않으면
            // 토핑 추가 버튼이 왜 안 눌리는지까지 보이지 않는다
            assertIs<CanvasMainEffect.ShowTodayCanvasError>(awaitItem())
        }
    }

    @Test
    fun enter_todayCanvasFailsAfterOneIsShown_staysQuiet() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 돌아오면서 저절로 나간 재조회가 실패한다
            coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(CanvasMainIntent.Enter)
            advanceUntilIdle()

            // Then 화면이 앞에 설 때마다 재조회하므로 매번 알리면 방해가 된다. 보여 줄
            // 캔버스가 남아 있으면 조용히 넘어간다
            expectNoEvents()
        }
        assertTrue(viewModel.state.value.todayCanvas != null)
    }

    @Test
    fun clickCamera_opensTheFlowWithTheCanvasItEnteredFrom() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 카메라로 떠난다
            viewModel.processIntent(CanvasMainIntent.OnClickCamera())
            advanceUntilIdle()

            // Then 초안을 쓴 뒤에야 화면이 옮겨 간다
            assertIs<CanvasMainEffect.NavigateToCamera>(awaitItem())
        }

        // 그리고 진입 시점의 캔버스가 초안에 못 박힌다 — 도중에 하루 경계를 넘어도 다른 캔버스로
        // 조용히 옮겨 가지 않는다. 토핑이 없는 캔버스라 다음 z 는 1 이다
        coVerify(exactly = 1) {
            toppingDraftRepository.start(
                groupId = GroupId(GROUP_ID),
                parfaitId = ParfaitId(TODAY_PARFAIT_ID),
                nextPositionZ = 1,
            )
        }
    }

    @Test
    fun clickGallery_opensTheFlowToo() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 갤러리로 떠난다
            viewModel.processIntent(CanvasMainIntent.OnClickCanvas())
            advanceUntilIdle()

            // Then 초안을 쓴 뒤에야 화면이 옮겨 간다
            assertIs<CanvasMainEffect.NavigateToCanvas>(awaitItem())
        }

        // 카메라와 같은 흐름이라 초안도 같이 열린다
        coVerify(exactly = 1) { toppingDraftRepository.start(any(), any(), any()) }
    }

    @Test
    fun clickCamera_stacksTheNewToppingOnTop() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑이 z 3 과 7 로 놓여 있는 오늘 캔버스
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(3), topping(7))),
        )
        val viewModel = enteredViewModel()

        // When 카메라로 떠난다
        viewModel.processIntent(CanvasMainIntent.OnClickCamera())
        advanceUntilIdle()

        // Then 맨 위 z 보다 하나 크다. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 겹친다
        coVerify(exactly = 1) {
            toppingDraftRepository.start(any(), any(), nextPositionZ = 8)
        }
    }

    @Test
    fun clickCamera_draftWriteFails_staysOnTheCanvasAndTellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안을 쓸 수 없는 상태
        coEvery { toppingDraftRepository.start(any(), any(), any()) } throws IOException("no space")
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 카메라로 떠나려 한다
            viewModel.processIntent(CanvasMainIntent.OnClickCamera())
            advanceUntilIdle()

            // Then 초안 쓰기가 실패하면 화면을 옮기지 않는다
            assertIs<CanvasMainEffect.ShowToppingFlowStartError>(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun clickCamera_withoutTodayCanvas_doesNotOpenTheFlow() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 못 받아 버튼이 잠긴 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = enteredViewModel()

        // When 그래도 의도가 들어온다(가드가 뚫렸거나 화면 밖에서 왔다)
        viewModel.processIntent(CanvasMainIntent.OnClickCamera())
        advanceUntilIdle()

        // Then 캔버스 식별값 없이 초안을 열지 않는다 — 그 초안으로는 올릴 데를 정할 수 없다
        coVerify(exactly = 0) { toppingDraftRepository.start(any(), any(), any()) }
    }

    @Test
    fun toppingAdd_isEnabledOnlyWhenTodayCanvasIsInHand() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 못 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val failed = enteredViewModel()

        // Then 올릴 데가 없으므로 잠근다
        assertFalse(failed.state.value.isToppingAddEnabled)

        // Given, When 캔버스를 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.success(canvas(TODAY_PARFAIT_ID, today))
        val loaded = enteredViewModel()

        // Then 열어 준다
        assertTrue(loaded.state.value.isToppingAddEnabled)
    }

    @Test
    fun enter_whileViewingAPastDate_dayChanges_clearsTodayCanvas() = runTest(mainDispatcherRule.dispatcher) {
        // Given 달력에서 어제로 옮겨 둔 화면
        mockkStatic(::parfaitToday)
        every { parfaitToday() } returns today
        val viewModel = enteredViewModel()
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        // When 화면을 켜 둔 채 하루 경계를 넘기고 다른 화면에서 돌아온다
        every { parfaitToday() } returns tomorrow
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        // Then 어제 캔버스가 오늘 것으로 오인되지 않도록 비워지고, 토핑 추가 버튼도 잠긴다
        val state = viewModel.state.value
        assertTrue(state.todayCanvas == null)
        assertFalse(state.isToppingAddEnabled)
    }

    @Test
    fun clickGoToToday_afterTodayCanvasWasCleared_reloadsIt() = runTest(mainDispatcherRule.dispatcher) {
        // Given 어제를 보는 중 하루 경계를 넘겨 todayCanvas 가 빈 화면
        mockkStatic(::parfaitToday)
        every { parfaitToday() } returns today
        val viewModel = enteredViewModel()
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        every { parfaitToday() } returns tomorrow
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()

        // When "오늘의 파르페 가기"를 누른다
        coEvery { getTodayParfait(any()) } returns Result.success(canvas(TODAY_PARFAIT_ID, tomorrow))
        viewModel.processIntent(CanvasMainIntent.OnClickGoToToday)
        advanceUntilIdle()

        // Then 오늘 조회가 다시 나가 토핑 추가 버튼이 다시 열린다
        coVerify(exactly = 2) { getTodayParfait(any()) }
        assertTrue(viewModel.state.value.isToppingAddEnabled)
    }

    private companion object {
        const val GROUP_ID = 7L
        const val TODAY_PARFAIT_ID = 42L
        const val YESTERDAY_PARFAIT_ID = 41L
        const val NEW_MEMBER_NICKNAME = "모카"

        val GROUP = MyParfaitGroupVO(
            groupId = GroupId(GROUP_ID),
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = NametagChipType.DEFAULT,
        )

        fun member(
            nickname: String,
            nametagChip: NametagChipType = NametagChipType.DEFAULT,
        ) = CanvasMemberVO(
            groupMemberId = GroupMemberId(1L),
            nickname = GroupNickname(nickname),
            nametagChip = nametagChip,
        )

        fun canvas(
            parfaitId: Long,
            date: LocalDate,
            members: List<CanvasMemberVO> = emptyList(),
        ) = CanvasVO(
            parfaitId = ParfaitId(parfaitId),
            date = date,
            status = CanvasStatus.ACTIVE,
            lastClosedDate = null,
            members = members,
            background = null,
            toppings = emptyList(),
        )

        fun topping(positionZ: Int) = CanvasToppingVO(
            parfaitImageId = ParfaitImageId(positionZ.toLong()),
            imageId = ImageId(positionZ.toLong()),
            imageUrl = "https://cdn.example.com/topping-$positionZ.png",
            transform = ToppingTransform(
                positionX = 0.5,
                positionY = 0.5,
                positionZ = positionZ,
                scale = 1.0,
                rotation = 0.0,
            ),
            border = ToppingBorder.None,
            placedBy = ToppingPlacerVO(
                groupMemberId = GroupMemberId(1L),
                nickname = GroupNickname("연경이"),
            ),
            createdAt = LocalDateTime(2026, 8, 20, 12, 0),
        )
    }
}
