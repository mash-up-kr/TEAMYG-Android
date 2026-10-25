package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasTextInputViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()

    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase = mockk()

    private val clearToppingDraft: ClearToppingDraftUseCase = mockk(relaxed = true)

    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private fun draft() = ToppingDraft(groupId = GroupId(1L), parfaitId = ParfaitId(2L), nextPositionZ = 1)

    private fun viewModel(): CanvasTextInputViewModel {
        every { getToppingDraftFlow() } returns flowOf(draft())
        every { getTodayParfaitFlowUseCase(any(), any()) } returns flowOf<CanvasVO?>(null)
        return CanvasTextInputViewModel(
            getToppingDraftFlow = getToppingDraftFlow,
            getTodayParfaitFlowUseCase = getTodayParfaitFlowUseCase,
            clearToppingDraft = clearToppingDraft,
            analyticsLogger = analyticsLogger,
        )
    }

    @Test
    fun onContentChange_withinLimit_keepsContentAsIs() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("hello"))

        assertEquals("hello", viewModel.state.value.content)
    }

    @Test
    fun onContentChange_beyondLimit_truncatesToGraphemeClusterLimit() {
        val viewModel = viewModel()
        val overLong = "a".repeat(TEXT_TOPPING_MAX_LENGTH + 10)

        viewModel.processIntent(CanvasTextInputIntent.OnContentChange(overLong))

        assertEquals(TEXT_TOPPING_MAX_LENGTH, viewModel.state.value.characterCount)
    }

    @Test
    fun onClickConfirm_blankContent_navigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("   \n  "))

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickConfirm)

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun onClickConfirm_nonBlankContent_doesNotNavigate() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("텍스트를 입력 중인 상태"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickConfirm)

            expectNoEvents()
        }
    }

    @Test
    fun onClickClose_opensQuitDialogWithoutNavigating() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickClose)

            expectNoEvents()
        }
        assertTrue(viewModel.state.value.showQuitDialog)
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun onQuitDialogConfirm_clearsDraftAndNavigatesToCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnClickClose)

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogConfirm)

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.showQuitDialog)
        coVerify { clearToppingDraft() }
    }

    @Test
    fun onQuitDialogCancel_closesDialogWithoutNavigating() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnClickClose)

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogCancel)

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.showQuitDialog)
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun onClickStack_clearsDraftAndNavigatesToCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
        coVerify { clearToppingDraft() }
    }

    @Test
    fun isAtLimit_exactlyAtMax_isTrue() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("a".repeat(TEXT_TOPPING_MAX_LENGTH)))

        assertEquals(true, viewModel.state.value.isAtLimit)
    }

    @Test
    fun isAtLimit_belowMax_isFalse() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("hi"))

        assertFalse(viewModel.state.value.isAtLimit)
    }

    @Test
    fun onSelectColor_updatesSelectedColor() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnSelectColor(TextToppingColor.Black))

        assertEquals(TextToppingColor.Black, viewModel.state.value.selectedColor)
    }

    @Test
    fun onToggleColorPopover_togglesOpenState() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnToggleColorPopover)
        assertEquals(true, viewModel.state.value.isColorPopoverOpen)

        viewModel.processIntent(CanvasTextInputIntent.OnToggleColorPopover)
        assertEquals(false, viewModel.state.value.isColorPopoverOpen)
    }

    @Test
    fun onDismissColorPopover_closesOpenPopover_andStaysClosed() {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnToggleColorPopover)

        viewModel.processIntent(CanvasTextInputIntent.OnDismissColorPopover)
        assertEquals(false, viewModel.state.value.isColorPopoverOpen)

        viewModel.processIntent(CanvasTextInputIntent.OnDismissColorPopover)
        assertEquals(false, viewModel.state.value.isColorPopoverOpen)
    }

    @Test
    fun onAutoFontSizeChange_keepsLastReportedDesignFontSize() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnAutoFontSizeChange(18.5f))

        assertEquals(18.5f, viewModel.state.value.designFontSize)
    }

    @Test
    fun onClickType_cyclesDefaultToFillToFillStrokeToDefault() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnClickType)
        assertEquals(TextToppingType.FILL, viewModel.state.value.textType)

        viewModel.processIntent(CanvasTextInputIntent.OnClickType)
        assertEquals(TextToppingType.FILL_STROKE, viewModel.state.value.textType)

        viewModel.processIntent(CanvasTextInputIntent.OnClickType)
        assertEquals(TextToppingType.DEFAULT, viewModel.state.value.textType)
    }

    @Test
    fun onClickAlign_cyclesCenterToLeftToRightToCenter() {
        val viewModel = viewModel()
        assertEquals(TextAlign.Center, viewModel.state.value.align)

        viewModel.processIntent(CanvasTextInputIntent.OnClickAlign)
        assertEquals(TextAlign.Left, viewModel.state.value.align)

        viewModel.processIntent(CanvasTextInputIntent.OnClickAlign)
        assertEquals(TextAlign.Right, viewModel.state.value.align)

        viewModel.processIntent(CanvasTextInputIntent.OnClickAlign)
        assertEquals(TextAlign.Center, viewModel.state.value.align)
    }

    @Test
    fun onToppingTransform_appliesPanZoomAndRotationTogether() {
        val viewModel = viewModel()

        viewModel.processIntent(
            CanvasTextInputIntent.OnToppingTransform(pan = DpOffset(10.dp, 20.dp), zoom = 1.5f, rotationDelta = 15f),
        )

        val state = viewModel.state.value
        assertEquals(10.dp, state.offsetX)
        assertEquals(20.dp, state.offsetY)
        assertEquals(1.5f, state.scale)
        assertEquals(15f, state.rotationDegrees)
        assertTrue(state.hasUserAdjustedPlacement)
    }

    @Test
    fun onDragPlacementSeed_setsOffsetAndMarksUserAdjusted() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnDragPlacementSeed(offsetX = 30.dp, offsetY = 40.dp))

        val state = viewModel.state.value
        assertEquals(30.dp, state.offsetX)
        assertEquals(40.dp, state.offsetY)
        assertTrue(state.hasUserAdjustedPlacement)
    }

    @Test
    fun onDragPlacementSeed_thenMeasure_doesNotRecenter() {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnDragPlacementSeed(offsetX = 30.dp, offsetY = 40.dp))

        viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(300.dp, 500.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(100.dp, 40.dp)))

        val state = viewModel.state.value
        assertEquals(30.dp, state.offsetX)
        assertEquals(40.dp, state.offsetY)
    }

    @Test
    fun onToppingTransform_zoomOut_doesNotGoBelowMinScale() {
        val viewModel = viewModel()

        repeat(20) {
            viewModel.processIntent(
                CanvasTextInputIntent.OnToppingTransform(pan = DpOffset.Zero, zoom = 0.1f, rotationDelta = 0f),
            )
        }

        assertEquals(TEXT_TOPPING_MIN_SCALE, viewModel.state.value.scale)
    }

    @Test
    fun measuringCanvasAndTextBox_beforeUserAdjusts_centersTextOnCanvas() {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(300.dp, 500.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(100.dp, 40.dp)))

        val state = viewModel.state.value
        assertEquals(100.dp, state.offsetX)
        assertEquals(230.dp, state.offsetY)
    }

    @Test
    fun measuringCanvasAndTextBox_afterUserAdjusted_doesNotOverrideManualPlacement() {
        val viewModel = viewModel()
        viewModel.processIntent(
            CanvasTextInputIntent.OnToppingTransform(pan = DpOffset(5.dp, 5.dp), zoom = 1f, rotationDelta = 0f),
        )

        viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(300.dp, 500.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(100.dp, 40.dp)))

        val state = viewModel.state.value
        assertEquals(5.dp, state.offsetX)
        assertEquals(5.dp, state.offsetY)
    }

    @Test
    fun init_readsGroupIdFromDraft_toObserveCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        // launchWhileSubscribed 는 state 구독자 수로 캔버스 구독 수명을 잰다 — 라우트의
        // collectAsStateWithLifecycle() 을 흉내 내 여기서 먼저 구독을 붙여야 열린다
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        verify { getTodayParfaitFlowUseCase(GroupId(1L), any()) }
    }
}
