package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.parfait.PastCanvasAlertRepository
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasToGalleryUseCase
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsFlowUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshMyGroupsUseCase
import com.teamyg.parfait.domain.usecase.member.CompleteTutorialUseCase
import com.teamyg.parfait.domain.usecase.member.GetTutorialVisibleFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitHistoriesUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitYearsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveParfaitDayBoundaryUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveTodayParfaitRefreshFailureUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasTutorialStep
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasMainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getParfaitHistories: GetParfaitHistoriesUseCase = mockk()
    private val getParfaitYears: GetParfaitYearsUseCase = mockk()
    private val getTodayParfaitFlow: GetTodayParfaitFlowUseCase = mockk()
    private val observeParfaitDayBoundary: ObserveParfaitDayBoundaryUseCase = mockk()
    private val observeTodayParfaitRefreshFailure: ObserveTodayParfaitRefreshFailureUseCase = mockk()
    private val getParfaitDetail: GetParfaitDetailUseCase = mockk()
    private val getMyGroupsFlow: GetMyGroupsFlowUseCase = mockk()
    private val refreshMyGroups: RefreshMyGroupsUseCase = mockk()
    private val saveCanvasToGallery: SaveCanvasToGalleryUseCase = mockk()
    private val getTutorialVisible: GetTutorialVisibleFlowUseCase = mockk()
    private val completeTutorial: CompleteTutorialUseCase = mockk(relaxed = true)

    private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxUnitFun = true)
    private val pastCanvasAlertRepository: PastCanvasAlertRepository = mockk(relaxUnitFun = true)

    /** 저장소의 오늘 캔버스 캐시. 갱신이 성공했다는 것은 여기에 값이 실린다는 뜻이다 */
    private val todayCanvases = MutableStateFlow<CanvasVO?>(null)

    /** 폴러의 갱신 실패 신호. 캐시는 실패했을 때 아무것도 방출하지 않아 이 축이 따로 있다 */
    private val refreshFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 기본값은 "이미 본 사용자" — 튜토리얼이 다른 테스트의 화면 상태에 끼어들지 않게 한다 */
    private val canvasTutorialVisible = MutableStateFlow(false)

    private val today = parfaitToday()

    private val yesterday = today.minus(DatePeriod(days = 1))

    private val dayBeforeYesterday = today.minus(DatePeriod(days = 2))

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
                    status = CanvasStatus.CLOSED,
                    thumbnailUrl = null,
                    toppingCount = 1,
                ),
            ),
        )
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        every { observeParfaitDayBoundary(any()) } returns flowOf(today)
        every { observeTodayParfaitRefreshFailure(any()) } returns refreshFailures
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
        coEvery { getParfaitDetail(any(), any()) } returns Result.success(canvas(YESTERDAY_PARFAIT_ID, yesterday))
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUP))
        coEvery { refreshMyGroups() } returns Result.success(Unit)
        every { getTutorialVisible(TutorialKind.CANVAS) } returns canvasTutorialVisible
        coEvery { pastCanvasAlertRepository.lastSeenClosedDate(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun viewModel(
        welcomeGroupName: String? = null,
        welcomeInviteCode: String? = null,
    ) = CanvasMainViewModel(
        groupIdValue = GROUP_ID,
        welcomeGroupName = welcomeGroupName,
        welcomeInviteCode = welcomeInviteCode,
        getParfaitHistoriesUseCase = getParfaitHistories,
        getParfaitYearsUseCase = getParfaitYears,
        getTodayParfaitFlowUseCase = getTodayParfaitFlow,
        observeParfaitDayBoundaryUseCase = observeParfaitDayBoundary,
        observeTodayParfaitRefreshFailureUseCase = observeTodayParfaitRefreshFailure,
        getParfaitDetailUseCase = getParfaitDetail,
        getMyGroupsFlowUseCase = getMyGroupsFlow,
        refreshMyGroupsUseCase = refreshMyGroups,
        saveCanvasToGalleryUseCase = saveCanvasToGallery,
        getTutorialVisibleFlowUseCase = getTutorialVisible,
        completeTutorialUseCase = completeTutorial,
        toppingDraftRepository = toppingDraftRepository,
        pastCanvasAlertRepository = pastCanvasAlertRepository,
    )

    /**
     * 대부분의 테스트는 화면이 떠 있는 상태에서 시작한다. `launchWhileSubscribed` 는 [state] 의
     * 구독자 수로 폴링 수명을 재므로, 라우트의 `collectAsStateWithLifecycle()` 을 흉내 내
     * 여기서 먼저 구독을 붙여야 오늘 캔버스·하루 경계 구독이 열린다.
     */
    private fun TestScope.enteredViewModel() = viewModel().also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        viewModel.processIntent(CanvasMainIntent.Enter)
        advanceUntilIdle()
    }

    @Test
    fun tutorial_onFirstLaunch_opensAtTheFirstStep() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 튜토리얼을 보지 않은 사용자
        canvasTutorialVisible.value = true

        // When 캔버스 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 첫 장이 뜬다
        assertEquals(CanvasTutorialStep.first, viewModel.state.value.tutorialStep)
    }

    @Test
    fun tutorial_alreadySeen_doesNotOpen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 튜토리얼을 끝낸 사용자(기본 스텁)

        // When 캔버스 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 아무것도 덮지 않는다
        assertNull(viewModel.state.value.tutorialStep)
    }

    @Test
    fun tutorial_clickingNextBeforeTheLastStep_doesNotMarkItSeen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 첫 장이 떠 있다
        canvasTutorialVisible.value = true
        val viewModel = enteredViewModel()

        // When 칩을 한 번 누른다
        viewModel.processIntent(CanvasMainIntent.OnClickTutorialNext)
        advanceUntilIdle()

        // Then 다음 장으로 넘어갈 뿐, 중간에 끊긴 튜토리얼을 봤다고 남기지 않는다
        assertEquals(CanvasTutorialStep.first.next, viewModel.state.value.tutorialStep)
        coVerify(exactly = 0) { completeTutorial(TutorialKind.CANVAS) }
    }

    @Test
    fun tutorial_clickingNextOnTheLastStep_closesItAndMarksItSeen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 튜토리얼이 떠 있다
        canvasTutorialVisible.value = true
        val viewModel = enteredViewModel()

        // When 마지막 장까지 칩을 눌러 넘긴다
        repeat(CanvasTutorialStep.totalCount) {
            viewModel.processIntent(CanvasMainIntent.OnClickTutorialNext)
            advanceUntilIdle()
        }

        // Then 튜토리얼이 닫히고, 다음 진입부터 뜨지 않도록 저장한다
        assertNull(viewModel.state.value.tutorialStep)
        coVerify(exactly = 1) { completeTutorial(TutorialKind.CANVAS) }
    }

    @Test
    fun enter_loadsTodayCanvasAndThisYearsHistories() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 화면이 열린다
        val viewModel = enteredViewModel()

        // Then 오늘 캔버스를 그리고, 달력이 점을 찍을 올해 기록도 받아 둔다
        val state = viewModel.state.value
        assertEquals(ParfaitId(TODAY_PARFAIT_ID), state.displayedCanvas?.parfaitId)
        assertEquals(setOf(yesterday), state.uploadedDates)
        coVerify(exactly = 1) { getParfaitHistories(any(), today.year) }
    }

    @Test
    fun enter_beforeTheScreenIsShown_doesNotLoadTheCanvas() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장소 캐시가 비어 있다(콜드 스타트)
        todayCanvases.value = null

        // When ViewModel 만 만들어지고 [state] 를 구독하는 화면이 아직 없다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then launchWhileSubscribed 가 업스트림을 열지 않아 폴링도 시작되지 않는다
        verify(exactly = 0) { getTodayParfaitFlow(any(), any()) }
        val state = viewModel.state.value
        assertTrue(state.displayedCanvas == null)
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

        // Then 마감된 날은 더 바뀌지 않으므로 달력 기록을 다시 묻지 않고, 보던 날도 그대로다
        val state = viewModel.state.value
        assertEquals(yesterday, state.selectedDate)
        assertEquals(ParfaitId(YESTERDAY_PARFAIT_ID), state.displayedCanvas?.parfaitId)
        coVerify(exactly = 1) { getParfaitHistories(any(), any()) }
    }

    @Test
    fun observeTodayCanvas_emission_landsOnTheScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 있다
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        // When 다른 화면이 토핑을 올려 저장소의 오늘 캔버스가 바뀐다
        val canvas = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(1)))
        todayCanvases.value = canvas
        advanceUntilIdle()

        // Then 이 화면이 다시 조회하지 않아도 그 자리에서 따라온다
        assertEquals(canvas, viewModel.state.value.todayCanvas)
        assertEquals(canvas, viewModel.state.value.displayedCanvas)
    }

    @Test
    fun observeTodayCanvas_whileViewingAPastDate_doesNotCoverThatDay() = runTest(mainDispatcherRule.dispatcher) {
        // Given 달력에서 어제로 옮겨 둔 화면
        val viewModel = enteredViewModel()
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        // When 그 사이 오늘 캔버스가 새로 실린다
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(1)))
        advanceUntilIdle()

        // Then 보고 있던 날은 덮이지 않는다 — 오늘 것과 지난 날 것을 갈라 두는 이유다
        val state = viewModel.state.value
        assertEquals(ParfaitId(YESTERDAY_PARFAIT_ID), state.displayedCanvas?.parfaitId)
    }

    @Test
    fun observeTodayCanvas_whenTheSpotlightedToppingDisappears_resetsTheSpotlight() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 남이 올린 토핑을 강조해 둔 화면
            val othersTopping = topping(positionZ = 1, isMine = false)
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(othersTopping))
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()

            viewModel.processIntent(CanvasMainIntent.OnClickTopping(othersTopping))
            advanceUntilIdle()
            assertEquals(othersTopping.parfaitImageId, viewModel.state.value.spotlightedToppingId)

            // When 강조하던 토핑이 캔버스에서 사라진다
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
            advanceUntilIdle()

            // Then 딤을 지울 계기가 없어지므로 여기서 함께 해제한다
            assertNull(viewModel.state.value.spotlightedToppingId)
        }

    @Test
    fun observeTodayCanvas_whenTheSpotlightedToppingRemains_keepsTheSpotlight() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 남이 올린 토핑을 강조해 둔 화면
            val othersTopping = topping(positionZ = 1, isMine = false)
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(othersTopping))
            val viewModel = viewModel()
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()

            viewModel.processIntent(CanvasMainIntent.OnClickTopping(othersTopping))
            advanceUntilIdle()

            // When 남이 토핑을 하나 더 올려도 강조는 그대로다
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
                .copy(toppings = listOf(othersTopping, topping(positionZ = 2, isMine = false)))
            advanceUntilIdle()

            assertEquals(othersTopping.parfaitImageId, viewModel.state.value.spotlightedToppingId)
        }

    @Test
    fun dayBoundary_movesTodayAndTheSelectedDate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 하루 경계 티커를 직접 흔들 수 있다
        val days = MutableStateFlow(LocalDate(2026, 8, 27))
        every { observeParfaitDayBoundary(any()) } returns days

        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        // When 화면을 열어 둔 채 하루 경계를 넘긴다
        days.value = LocalDate(2026, 8, 28)
        advanceUntilIdle()

        // Then 오늘을 보고 있었으므로 today 와 selectedDate 가 함께 넘어간다
        assertEquals(LocalDate(2026, 8, 28), viewModel.state.value.today)
        assertEquals(LocalDate(2026, 8, 28), viewModel.state.value.selectedDate)
    }

    @Test
    fun clickDate_pastDate_beforeTheDetailArrives_doesNotLookEmpty() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑이 올라간 오늘 캔버스를 보고 있고, 상세 응답은 오지 않는다
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(1)))
        coEvery { getParfaitDetail(any(), any()) } coAnswers { awaitCancellation() }
        val viewModel = enteredViewModel()

        // When 달력에서 어제를 고른다
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        // Then 달력이 기록 있는 날만 열어 주므로 "비어 있다"는 늘 거짓이다 — 상세가 올 때까지
        // 직전에 보던 것을 그대로 둔다
        assertFalse(viewModel.state.value.isCanvasEmpty)
    }

    @Test
    fun clickDate_returnToTodayThenAnotherPastDate_beforeDetailArrives_showsTodayNotThePreviousPastDate() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 어제(A)를 본 뒤 오늘로 돌아온 화면. 그저께(B)도 달력에 열려 있다
            coEvery { getParfaitHistories(any(), any()) } returns Result.success(
                listOf(
                    PastCanvasVO(
                        parfaitId = ParfaitId(YESTERDAY_PARFAIT_ID),
                        date = yesterday,
                        status = CanvasStatus.CLOSED,
                        thumbnailUrl = null,
                        toppingCount = 1,
                    ),
                    PastCanvasVO(
                        parfaitId = ParfaitId(DAY_BEFORE_YESTERDAY_PARFAIT_ID),
                        date = dayBeforeYesterday,
                        status = CanvasStatus.CLOSED,
                        thumbnailUrl = null,
                        toppingCount = 1,
                    ),
                ),
            )
            val viewModel = enteredViewModel()
            viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
            advanceUntilIdle()
            viewModel.processIntent(CanvasMainIntent.OnClickGoToToday)
            advanceUntilIdle()

            // When 그저께(B)를 고르고, 상세가 도착하기 전이다
            coEvery { getParfaitDetail(any(), any()) } coAnswers { awaitCancellation() }
            viewModel.processIntent(CanvasMainIntent.ClickDate(dayBeforeYesterday))
            advanceUntilIdle()

            // Then 헤더는 B 날짜지만, 캔버스는 어제(A) 것이 아니라 오늘 것이어야 한다 — 오늘로
            // 돌아올 때 pastCanvas 를 비우지 않으면 어제 캔버스가 계속 걸린다
            val state = viewModel.state.value
            assertEquals(dayBeforeYesterday, state.selectedDate)
            assertEquals(ParfaitId(TODAY_PARFAIT_ID), state.displayedCanvas?.parfaitId)
        }

    @Test
    fun todayCanvas_firstTimeCheckingThisGroup_seedsBaselineWithoutShowingAlert() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이 기기·이 그룹 조합을 한 번도 확인한 적이 없다(배포 직후 기존 그룹 포함)
            coEvery { pastCanvasAlertRepository.lastSeenClosedDate(GroupId(GROUP_ID)) } returns null
            val viewModel = enteredViewModel()

            viewModel.effect.test {
                // When 이미 마감 이력이 있는 그룹의 오늘 캔버스를 처음 받아 온다
                todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(lastClosedDate = yesterday)
                advanceUntilIdle()

                // Then 처음 확인하는 것이므로 기준선만 세우고 알리지 않는다 — 그러지 않으면
                // 몇 주 전에 이미 마감된 캔버스까지 배포 직후 첫 진입에서 알럿으로 뜬다
                expectNoEvents()
            }

            coVerify(exactly = 1) { pastCanvasAlertRepository.markSeen(GroupId(GROUP_ID), yesterday) }
        }

    @Test
    fun todayCanvas_lastClosedDateChangedFromPreviouslySeenDate_showsPastCanvasAlertAndMarksSeen() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이 그룹은 그저께까지는 확인한 적이 있고, 어제 캔버스에는 서로 다른 두 사람이
            // 토핑을 올렸다 — 오늘 시점 그룹원 수(예: 그 뒤 합류·탈퇴)와는 별개다
            coEvery { pastCanvasAlertRepository.lastSeenClosedDate(GroupId(GROUP_ID)) } returns dayBeforeYesterday
            coEvery { getParfaitDetail(any(), ParfaitId(YESTERDAY_PARFAIT_ID)) } returns Result.success(
                canvas(YESTERDAY_PARFAIT_ID, yesterday).copy(
                    toppings = listOf(
                        topping(1, placedByGroupMemberId = 1L),
                        topping(2, placedByGroupMemberId = 2L),
                    ),
                ),
            )
            val viewModel = enteredViewModel()

            viewModel.effect.test {
                // When 03시 회전으로 어제까지 마감된 오늘 캔버스를 받아 온다
                todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(lastClosedDate = yesterday)

                // Then 새로 마감된 날과, 그 날 실제로 토핑을 올린 사람 수를 실어 알린다
                assertEquals(
                    CanvasMainEffect.ShowPastCanvasAlert(date = yesterday, memberCount = 2),
                    awaitItem(),
                )
            }

            // 그리고 같은 마감일을 다시 보여주지 않도록 기록해 둔다
            coVerify(exactly = 1) { pastCanvasAlertRepository.markSeen(GroupId(GROUP_ID), yesterday) }
        }

    @Test
    fun todayCanvas_closedCanvasDetailFetchFails_doesNotShowPastCanvasAlert() = runTest(mainDispatcherRule.dispatcher) {
        // Given 참여자 수를 세려고 어제 캔버스를 조회하는데 실패한다
        coEvery { pastCanvasAlertRepository.lastSeenClosedDate(GroupId(GROUP_ID)) } returns dayBeforeYesterday
        coEvery { getParfaitDetail(any(), ParfaitId(YESTERDAY_PARFAIT_ID)) } returns
            Result.failure(IOException("network"))
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(lastClosedDate = yesterday)
            advanceUntilIdle()

            // Then 정확한 인원 수를 모르니 알리지 않는다 — 틀린 숫자를 보여주는 것보다 낫다
            expectNoEvents()
        }
    }

    @Test
    fun todayCanvas_lastClosedDateOlderThanYesterday_doesNotShowAlertButStillMarksSeen() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 며칠 앱을 안 연 사이(또는 중간에 토핑 0건인 날이 껴서), 새로 확인한
            // 마감일이 어제가 아니라 그저께다
            val threeDaysAgo = today.minus(DatePeriod(days = 3))
            coEvery { pastCanvasAlertRepository.lastSeenClosedDate(GroupId(GROUP_ID)) } returns threeDaysAgo
            val viewModel = enteredViewModel()

            viewModel.effect.test {
                // When 오늘 캔버스의 마감일이 그저께로 갱신된다 — 어제는 아니다
                todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(lastClosedDate = dayBeforeYesterday)
                advanceUntilIdle()

                // Then "방금 회전됐다"는 전제가 깨지므로 알리지 않는다
                expectNoEvents()
            }

            // 그래도 다음 확인 때 다시 검사하지 않도록 기준은 갱신해 둔다
            coVerify(exactly = 1) { pastCanvasAlertRepository.markSeen(GroupId(GROUP_ID), dayBeforeYesterday) }
        }

    @Test
    fun todayCanvas_lastClosedDateAlreadySeen_doesNotShowPastCanvasAlertAgain() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이 그룹은 이 마감일을 이미 확인했다
            coEvery { pastCanvasAlertRepository.lastSeenClosedDate(GroupId(GROUP_ID)) } returns yesterday
            val viewModel = enteredViewModel()

            viewModel.effect.test {
                // When 폴링이 같은 마감일을 다시 흘린다 — 5초마다 도는 폴링이면 매번 일어난다
                todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(lastClosedDate = yesterday)
                advanceUntilIdle()

                // Then 이미 봤으므로 조용하다
                expectNoEvents()
            }

            // 이미 같은 값이므로 다시 기록할 이유도 없다
            coVerify(exactly = 0) { pastCanvasAlertRepository.markSeen(any(), any()) }
        }

    @Test
    fun clickPastCanvasAlertDate_yearAlreadyCached_navigatesWithoutFetching() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 어제가 속한 해의 기록을 이미 캐시해 뒀다(stubTheHappyPath)
        val viewModel = enteredViewModel()

        // When 알럿의 "보러가기"를 누른다
        viewModel.processIntent(CanvasMainIntent.ClickPastCanvasAlertDate(yesterday))
        advanceUntilIdle()

        // Then 그 해를 다시 받아오지 않고 바로 그 날로 옮긴다
        assertEquals(yesterday, viewModel.state.value.selectedDate)
        coVerify(exactly = 1) { getParfaitHistories(any(), any()) }
    }

    @Test
    fun clickPastCanvasAlertDate_yearNotCached_fetchesThatYearThenNavigates() = runTest(mainDispatcherRule.dispatcher) {
        // Given 마감일이 아직 달력에 펼쳐 보지 않은(캐시에 없는) 해에 속한다
        val oldDate = LocalDate(today.year - 1, 12, 31)
        val oldParfaitId = ParfaitId(999L)
        coEvery { getParfaitHistories(any(), today.year - 1) } returns Result.success(
            listOf(
                PastCanvasVO(
                    parfaitId = oldParfaitId,
                    date = oldDate,
                    status = CanvasStatus.CLOSED,
                    thumbnailUrl = null,
                    toppingCount = 1,
                ),
            ),
        )
        coEvery { getParfaitDetail(any(), oldParfaitId) } returns Result.success(canvas(999L, oldDate))
        val viewModel = enteredViewModel()

        // When 알럿의 "보러가기"를 누른다
        viewModel.processIntent(CanvasMainIntent.ClickPastCanvasAlertDate(oldDate))
        advanceUntilIdle()

        // Then 그 해만 따로 받아온 뒤 그 날로 옮긴다
        coVerify(exactly = 1) { getParfaitHistories(any(), today.year - 1) }
        assertEquals(oldDate, viewModel.state.value.selectedDate)
        assertEquals(
            oldParfaitId,
            viewModel.state.value.pastCanvas
                ?.parfaitId,
        )
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
    fun init_cacheAndCanvasDisagree_prefersCachedName() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스 응답과 목록 캐시가 서로 다른 이름을 들고 있다
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(groupName = GroupName("카페라떼"))
        every { getMyGroupsFlow() } returns flowOf(listOf(GROUP))

        // When 화면이 열려 캔버스 구독이 붙는다
        val viewModel = enteredViewModel()

        // Then 그룹명의 정본인 목록 쪽 이름이 온다
        assertEquals("아메리카노", viewModel.state.value.groupName)
    }

    @Test
    fun init_cacheEmpty_bootstrapsGroupNameFromCanvas() = runTest(mainDispatcherRule.dispatcher) {
        // Given 목록 캐시가 비어 있고(푸시로 바로 들어온 진입) 캔버스만 이름을 들고 있다
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(groupName = GroupName("카페라떼"))
        every { getMyGroupsFlow() } returns flowOf(null)
        coEvery { refreshMyGroups() } returns Result.success(Unit)

        // When 화면이 열려 캔버스 구독이 붙는다
        val viewModel = enteredViewModel()

        // Then 상단 바가 비지 않는다
        assertEquals("카페라떼", viewModel.state.value.groupName)
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
        todayCanvases.value = canvas(
            TODAY_PARFAIT_ID,
            today,
            members = listOf(
                member("모카", NametagChipType.TYPE7),
                member("판다", NametagChipType.TYPE2),
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
        todayCanvases.value = canvas(
            TODAY_PARFAIT_ID,
            today,
            members = listOf(member("판다", NametagChipType.TYPE2)),
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
        todayCanvases.value = canvas(
            TODAY_PARFAIT_ID,
            today,
            members = listOf(member("모카", nametagChip = NametagChipType.DEFAULT)),
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
    fun onClickSaveToGallery_requestsCanvasCaptureForPreview() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열린 상태
        val viewModel = enteredViewModel()

        // When 저장 버튼을 누른다
        viewModel.effect.test {
            // Then 바로 저장하지 않는다 — 미리보기로 넘길 캡처만 요청한다
            viewModel.processIntent(CanvasMainIntent.OnClickSaveToGallery)

            assertEquals(CanvasMainEffect.RequestCanvasCaptureForPreview, awaitItem())
        }
    }

    @Test
    fun saveCapturedCanvas_useCaseSucceeds_showsSuccessWithTheViewedDate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열린 상태(오늘을 보고 있다)이고 저장이 성공한다
        val viewModel = enteredViewModel()
        coEvery { saveCanvasToGallery(any(), any()) } returns Result.success(Unit)

        // When 화면이 캡처한 비트맵을 돌려준다
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(mockk<Bitmap>()))

            // Then 지금 보고 있는 날짜와 함께 성공을 알린다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = true, date = today),
                awaitItem(),
            )
        }
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
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(3), topping(7)))
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
    fun clickTopping_placedBySomeoneElse_opensSpotlightWithTheAuthorToast() = runTest(mainDispatcherRule.dispatcher) {
        // Given 남이 올린 토핑 하나가 놓여 있다
        val othersTopping = topping(positionZ = 1, isMine = false)
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(othersTopping))
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 그 토핑을 탭한다
            viewModel.processIntent(CanvasMainIntent.OnClickTopping(othersTopping))
            advanceUntilIdle()

            // Then 강조와 함께 작성자를 알린다
            assertIs<CanvasMainEffect.ShowSpotlightToast>(awaitItem())
        }
        assertEquals(othersTopping.parfaitImageId, viewModel.state.value.spotlightedToppingId)
    }

    @Test
    fun clickTopping_placedByMe_navigatesToCanvasBGEditInsteadOfSpotlighting() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 서버가 내 것으로 판정한 토핑이 놓여 있다
            val myTopping = topping(positionZ = 1, isMine = true)
            todayCanvases.value = canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(myTopping))
            val viewModel = enteredViewModel()

            // When 그 토핑을 탭한다
            viewModel.effect.test {
                viewModel.processIntent(CanvasMainIntent.OnClickTopping(myTopping))
                advanceUntilIdle()

                // Then Spotlight 가 아니라 C-305 토핑 편집으로, 탭한 토핑 id 를 실어 보낸다
                assertEquals(
                    CanvasMainEffect.NavigateToCanvasBGEdit(
                        groupId = GroupId(GROUP_ID),
                        parfaitId = ParfaitId(TODAY_PARFAIT_ID),
                        toppingId = myTopping.parfaitImageId,
                    ),
                    awaitItem(),
                )
            }
            assertNull(viewModel.state.value.spotlightedToppingId)
        }

    @Test
    fun clickTopping_placedByMe_whileViewingAPastDate_doesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 지난 날의 내 토핑을 보고 있다 — CanvasBGEdit 은 넘겨받은 parfaitId 와 무관하게
        // 항상 오늘 캔버스를 다시 조회하므로, 여기서 열면 지난 날이 아니라 오늘 것이 열린다
        val myTopping = topping(positionZ = 1, isMine = true)
        coEvery { getParfaitDetail(any(), any()) } returns Result.success(
            canvas(YESTERDAY_PARFAIT_ID, yesterday).copy(toppings = listOf(myTopping)),
        )
        val viewModel = enteredViewModel()
        viewModel.processIntent(CanvasMainIntent.ClickDate(yesterday))
        advanceUntilIdle()

        // When 그 토핑을 탭한다
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickTopping(myTopping))
            advanceUntilIdle()

            // Then 아무 반응도 없다 — Spotlight 도, 편집 화면 이동도 아니다
            expectNoEvents()
        }
        assertNull(viewModel.state.value.spotlightedToppingId)
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
        todayCanvases.value = null
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
        todayCanvases.value = null
        val failed = enteredViewModel()

        // Then 올릴 데가 없으므로 잠근다
        assertFalse(failed.state.value.isToppingAddEnabled)

        // Given, When 캔버스를 받은 화면
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
        val loaded = enteredViewModel()

        // Then 열어 준다
        assertTrue(loaded.state.value.isToppingAddEnabled)
    }

    @Test
    fun clickCanvasEdit_emitsTheCanvasBeingEdited() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 띄운 화면
        val viewModel = enteredViewModel()

        // When 캔버스 편집
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickCanvasEdit())

            // Then 편집 화면은 대상 캔버스를 알아야 한다 — 스스로 오늘 조회를 부르면 캔버스가
            // 없는 날에는 서버가 하나 더 만든다
            assertEquals(
                CanvasMainEffect.NavigateToCanvasBGEdit(
                    groupId = GroupId(GROUP_ID),
                    parfaitId = ParfaitId(TODAY_PARFAIT_ID),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun saveCapturedCanvas_useCaseFails_showsFailure() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열린 상태이고 저장이 실패한다
        val viewModel = enteredViewModel()
        coEvery { saveCanvasToGallery(any(), any()) } returns Result.failure(RuntimeException("저장 실패"))

        // When 화면이 캡처한 비트맵을 돌려준다
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(mockk<Bitmap>()))

            // Then 실패를 알린다 — 크래시 대신 토스트로 이어진다
            assertEquals(
                CanvasMainEffect.ShowGallerySaveResult(isSuccess = false, date = today),
                awaitItem(),
            )
        }
    }

    @Test
    fun clickCanvasEdit_beforeTheCanvasArrives_doesNotOpenTheEditor() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 아직 못 받은 화면
        todayCanvases.value = null
        val viewModel = enteredViewModel()

        // When 캔버스 편집
        viewModel.effect.test {
            viewModel.processIntent(CanvasMainIntent.OnClickCanvasEdit())

            // Then 없는 id 를 지어내 남의 날 캔버스를 고치게 두지 않는다
            expectNoEvents()
        }
    }

    @Test
    fun init_withoutWelcomeGroupName_showsNoWelcome() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 그룹 목록에서 평범하게 진입한 화면(환영 문구 없음)
        val viewModel = viewModel()

        // Then 환영 배너 효과가 오지 않는다
        viewModel.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun init_withWelcomeGroupNameOnly_showsJoinedWelcome() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 초대코드 없이 그룹명만 실려 온 진입(기존 그룹 참여 직후)
        viewModel(welcomeGroupName = GROUP_NAME).effect.test {
            // Then 참여 환영 배너를 1회 띄운다
            assertEquals(
                CanvasMainEffect.ShowWelcome(CanvasWelcome.Joined(groupName = GROUP_NAME)),
                awaitItem(),
            )
        }
    }

    @Test
    fun init_withWelcomeGroupNameAndInviteCode_showsCreatedWelcome() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 그룹명과 초대코드가 함께 실려 온 진입(그룹 생성 직후)
        viewModel(welcomeGroupName = GROUP_NAME, welcomeInviteCode = INVITE_CODE).effect.test {
            // Then 생성 환영 배너(초대코드 포함)를 1회 띄운다
            assertEquals(
                CanvasMainEffect.ShowWelcome(
                    CanvasWelcome.Created(groupName = GROUP_NAME, inviteCode = INVITE_CODE),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun theScreenAppearsWithNoCanvasYet_showsLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캐시에 아직 실린 캔버스가 없다(콜드 스타트)
        todayCanvases.value = null

        // When 화면이 앞에 서서 오늘 캔버스 구독이 열린다
        val viewModel = enteredViewModel()

        // Then 빈 캔버스를 들여다보지 않도록 첫 캔버스가 실릴 때까지 덮는다
        assertTrue(viewModel.state.value.isInitialLoading)

        // 폴러가 받아 온 캔버스가 캐시에 실리면 덮개를 내린다
        todayCanvases.value = canvas(TODAY_PARFAIT_ID, today)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isInitialLoading)
    }

    @Test
    fun theScreenAppearsWithTheCanvasLoaded_doesNotShowLoading() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 이미 실린 오늘 캔버스를 들고 화면이 앞에 선다
        val viewModel = enteredViewModel()

        // Then 보고 있던 캔버스 위로 딤이 번쩍이지 않는다
        assertFalse(viewModel.state.value.isInitialLoading)
    }

    @Test
    fun refreshFails_whileWaitingForTheFirstCanvas_clearsLoadingAndTellsTheUser() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 캐시가 비어 있어 화면이 첫 조회를 기다린다
            todayCanvases.value = null
            val viewModel = enteredViewModel()
            assertTrue(viewModel.state.value.isInitialLoading)

            viewModel.effect.test {
                // When 갱신이 실패해 캐시에 아무것도 실리지 않는다
                refreshFailures.emit(Unit)
                advanceUntilIdle()

                // Then 덮개를 내리고 실패를 말한다 — 그러지 않으면 화면이 로딩에 갇힌다
                assertFalse(viewModel.state.value.isInitialLoading)
                assertIs<CanvasMainEffect.ShowTodayCanvasError>(awaitItem())
            }
        }

    @Test
    fun refreshFails_withTheCanvasAlreadyShown_saysNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 이미 띄운 화면 — 덮개가 걸려 있지 않다
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 폴링이 한 번 실패한다
            refreshFailures.emit(Unit)
            advanceUntilIdle()

            // Then 5초마다 도는 폴링의 실패로 토스트를 쌓지 않는다 — 화면은 이미 캔버스를 그렸다
            expectNoEvents()
        }
    }

    private companion object {
        const val GROUP_ID = 7L
        const val GROUP_NAME = "모카의 파르페"
        const val INVITE_CODE = "ABC123"
        const val TODAY_PARFAIT_ID = 42L
        const val YESTERDAY_PARFAIT_ID = 41L
        const val DAY_BEFORE_YESTERDAY_PARFAIT_ID = 40L

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
            groupName = GroupName("아메리카노"),
            date = date,
            status = CanvasStatus.ACTIVE,
            lastClosedDate = null,
            members = members,
            background = null,
            toppings = emptyList(),
        )

        fun topping(
            positionZ: Int,
            isMine: Boolean = false,
            placedByGroupMemberId: Long = 1L,
        ) = CanvasToppingVO(
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
                groupMemberId = GroupMemberId(placedByGroupMemberId),
                nickname = GroupNickname("연경이"),
            ),
            isMine = isMine,
            createdAt = LocalDateTime(2026, 8, 20, 12, 0),
        )
    }
}
