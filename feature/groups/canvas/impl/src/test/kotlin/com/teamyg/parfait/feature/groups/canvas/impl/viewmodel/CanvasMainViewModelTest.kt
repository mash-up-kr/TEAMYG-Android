package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasMemberVO
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitHistoriesUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitYearsUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanvasMainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addRecentImage: AddRecentImageUseCase = mockk()
    private val getParfaitHistories: GetParfaitHistoriesUseCase = mockk()
    private val getParfaitYears: GetParfaitYearsUseCase = mockk()
    private val getTodayParfait: GetTodayParfaitUseCase = mockk()
    private val getParfaitDetail: GetParfaitDetailUseCase = mockk()

    private val today = parfaitToday()

    private val yesterday = today.minus(DatePeriod(days = 1))

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
    }

    private fun viewModel() = CanvasMainViewModel(
        groupIdValue = GROUP_ID,
        addRecentImageUseCase = addRecentImage,
        getParfaitHistoriesUseCase = getParfaitHistories,
        getParfaitYearsUseCase = getParfaitYears,
        getTodayParfaitUseCase = getTodayParfait,
        getParfaitDetailUseCase = getParfaitDetail,
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

    private companion object {
        const val GROUP_ID = 7L
        const val TODAY_PARFAIT_ID = 42L
        const val YESTERDAY_PARFAIT_ID = 41L
        const val NEW_MEMBER_NICKNAME = "모카"

        fun member(nickname: String) = CanvasMemberVO(
            groupMemberId = GroupMemberId(1L),
            nickname = GroupNickname(nickname),
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
    }
}
