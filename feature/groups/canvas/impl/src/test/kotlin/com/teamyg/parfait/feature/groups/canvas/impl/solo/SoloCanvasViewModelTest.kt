package com.teamyg.parfait.feature.groups.canvas.impl.solo

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasToGalleryUseCase
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveParfaitDayBoundaryUseCase
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloSpaceUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.ResetSoloSpaceUseCase
import com.teamyg.parfait.domain.usecase.topping.StartToppingDraftUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.SoloDialog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SoloCanvasViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase = mockk()
    private val resetSoloSpace: ResetSoloSpaceUseCase = mockk()
    private val deleteSoloSpace: DeleteSoloSpaceUseCase = mockk()
    private val startToppingDraft: StartToppingDraftUseCase = mockk(relaxUnitFun = true)
    private val observeParfaitDayBoundary: ObserveParfaitDayBoundaryUseCase = mockk()
    private val saveCanvasToGallery: SaveCanvasToGalleryUseCase = mockk()
    private val saveCanvasVideoToGallery: SaveCanvasVideoToGalleryUseCase = mockk()

    /** 저장소의 혼자 공간 캔버스. null 은 아직 아무것도 읽지 못한 상태다 */
    private val soloCanvases = MutableStateFlow<Result<SoloCanvasVO>?>(Result.success(EMPTY_CANVAS))

    private val today = parfaitToday()

    @Before
    fun stubTheHappyPath() {
        every { getSoloCanvasFlow() } returns soloCanvases.filterNotNull()
        every { observeParfaitDayBoundary(any()) } returns flowOf(today)
        coEvery { resetSoloSpace() } returns Result.success(Unit)
        coEvery { deleteSoloSpace() } returns Result.success(Unit)
    }

    private fun viewModel() = SoloCanvasViewModel(
        title = SOLO_TITLE,
        getSoloCanvasFlow = getSoloCanvasFlow,
        resetSoloSpace = resetSoloSpace,
        deleteSoloSpace = deleteSoloSpace,
        startToppingDraft = startToppingDraft,
        observeParfaitDayBoundary = observeParfaitDayBoundary,
        saveCanvasToGallery = saveCanvasToGallery,
        saveCanvasVideoToGallery = saveCanvasVideoToGallery,
    )

    private fun TestScope.shownViewModel() = viewModel().also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    @Test
    fun init_emitsCanvasWithSoloTitleAndNoMembers() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 화면이 열린다
        val viewModel = shownViewModel()

        // Then 제목은 "내 공간"이고 멤버 칩 없이 오늘 날짜의 캔버스를 그린다
        val canvasState = viewModel.state.value.canvas
        assertEquals(SOLO_TITLE, canvasState.groupName)
        assertTrue(canvasState.memberChips.isEmpty())
        assertFalse(canvasState.isInitialLoading)
        val todayCanvas = canvasState.todayCanvas
        assertEquals(ParfaitId(0), todayCanvas?.parfaitId)
        assertEquals(SOLO_TITLE, todayCanvas?.groupName?.value)
        assertEquals(today, todayCanvas?.date)
        assertEquals(CanvasStatus.ACTIVE, todayCanvas?.status)
        assertNull(todayCanvas?.lastClosedDate)
        assertTrue(todayCanvas?.members.orEmpty().isEmpty())
        assertTrue(canvasState.isViewingToday)
    }

    @Test
    fun menuClick_opensMenu_thenResetConfirm_callsResetOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 있다
        val viewModel = shownViewModel()

        // When 햄버거 → 비우기 → 확인을 세 번 연타한다
        viewModel.processIntent(SoloCanvasIntent.ClickMenu)
        assertEquals(SoloDialog.Menu, viewModel.state.value.soloDialog)
        viewModel.processIntent(SoloCanvasIntent.ClickResetMenu)
        assertEquals(SoloDialog.ResetConfirm, viewModel.state.value.soloDialog)
        repeat(3) { viewModel.processIntent(SoloCanvasIntent.ConfirmReset) }

        // Then 팝업은 바로 닫히고 비우기는 한 번만 불린다
        assertNull(viewModel.state.value.soloDialog)
        advanceUntilIdle()
        coVerify(exactly = 1) { resetSoloSpace() }
        assertFalse(viewModel.state.value.isSoloActionRunning)
    }

    @Test
    fun resetFailure_emitsResetFailedToast() = runTest(mainDispatcherRule.dispatcher) {
        // Given 비우기가 실패한다
        coEvery { resetSoloSpace() } returns Result.failure(IOException("disk"))
        val viewModel = shownViewModel()

        viewModel.effect.test {
            // When 비우기를 확정한다
            viewModel.processIntent(SoloCanvasIntent.ClickResetMenu)
            viewModel.processIntent(SoloCanvasIntent.ConfirmReset)
            advanceUntilIdle()

            // Then 비우기 실패 토스트를 띄운다
            assertIs<SoloCanvasEffect.ShowResetFailed>(awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isSoloActionRunning)
    }

    @Test
    fun deleteSuccess_emitsCloseEffectOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 있다
        val viewModel = shownViewModel()

        viewModel.effect.test {
            // When 삭제하기를 확정하는 버튼을 연타한다
            viewModel.processIntent(SoloCanvasIntent.ClickDeleteMenu)
            assertEquals(SoloDialog.DeleteConfirm, viewModel.state.value.soloDialog)
            repeat(3) { viewModel.processIntent(SoloCanvasIntent.ConfirmDelete) }
            advanceUntilIdle()

            // Then 화면을 한 번만 닫는다
            assertIs<SoloCanvasEffect.Close>(awaitItem())
            expectNoEvents()
        }
        coVerify(exactly = 1) { deleteSoloSpace() }
    }

    @Test
    fun deleteFailure_emitsDeleteFailedToast() = runTest(mainDispatcherRule.dispatcher) {
        // Given 삭제가 실패한다
        coEvery { deleteSoloSpace() } returns Result.failure(IOException("disk"))
        val viewModel = shownViewModel()

        viewModel.effect.test {
            // When 삭제를 확정한다
            viewModel.processIntent(SoloCanvasIntent.ClickDeleteMenu)
            viewModel.processIntent(SoloCanvasIntent.ConfirmDelete)
            advanceUntilIdle()

            // Then 삭제 실패 토스트를 띄우고 화면은 닫지 않는다
            assertIs<SoloCanvasEffect.ShowDeleteFailed>(awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isSoloActionRunning)
    }

    @Test
    fun menuClickWhileActionRunning_doesNotOpenMenu() = runTest(mainDispatcherRule.dispatcher) {
        // Given 비우기가 끝나지 않고 돌고 있다
        coEvery { resetSoloSpace() } coAnswers { awaitCancellation() }
        val viewModel = shownViewModel()
        viewModel.processIntent(SoloCanvasIntent.ClickResetMenu)
        viewModel.processIntent(SoloCanvasIntent.ConfirmReset)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isSoloActionRunning)

        // When 햄버거를 누른다
        viewModel.processIntent(SoloCanvasIntent.ClickMenu)

        // Then 메뉴가 열리지 않는다
        assertNull(viewModel.state.value.soloDialog)
    }

    @Test
    fun canvasLoadFailure_stopsLoadingAndEmitsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장소가 캔버스를 읽지 못한다
        soloCanvases.value = null
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canvas.isInitialLoading)

        viewModel.effect.test {
            // When 읽기 실패가 온다
            soloCanvases.value = Result.failure(IOException("corrupted"))
            advanceUntilIdle()

            // Then 로딩을 내리고 에러 토스트를 띄운다
            assertIs<SoloCanvasEffect.ShowCanvasLoadError>(awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.canvas.isInitialLoading)
    }

    @Test
    fun cameraClick_startsSoloDraft() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 있다
        val viewModel = shownViewModel()

        viewModel.effect.test {
            // When 카메라를 고른다
            viewModel.processIntent(SoloCanvasIntent.ClickCamera)
            advanceUntilIdle()

            // Then 혼자 공간 대상으로 초안을 쓴 뒤 카메라로 간다
            assertIs<SoloCanvasEffect.NavigateToCamera>(awaitItem())
        }
        coVerify(exactly = 1) { startToppingDraft(target = ToppingDraftTarget.Solo, nextPositionZ = 1) }
    }

    @Test
    fun textClick_startsSoloDraftThenOpensTextInput() = runTest(mainDispatcherRule.dispatcher) {
        // Given 화면이 열려 있다
        val viewModel = shownViewModel()

        viewModel.effect.test {
            // When 텍스트를 고른다
            viewModel.processIntent(SoloCanvasIntent.ClickText)
            advanceUntilIdle()

            // Then 혼자 공간 대상으로 초안을 쓴 뒤 텍스트 입력으로 간다
            assertIs<SoloCanvasEffect.NavigateToTextInput>(awaitItem())
        }
        coVerify(exactly = 1) { startToppingDraft(target = ToppingDraftTarget.Solo, nextPositionZ = 1) }
    }

    private companion object {
        const val SOLO_TITLE = "내 공간"

        val EMPTY_CANVAS = SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList())
    }
}
