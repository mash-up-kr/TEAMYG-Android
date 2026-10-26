package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.PlaceTextToppingUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.PlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditResult
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditSession
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextEditSessionHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingStyleOf
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTextToppingTransform
import java.io.IOException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasTextInputViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()

    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase = mockk()

    private val clearToppingDraft: ClearToppingDraftUseCase = mockk(relaxed = true)

    private val placeTextTopping: PlaceTextToppingUseCase = mockk()

    private val refreshTodayParfaitDetail: RefreshTodayParfaitDetailUseCase = mockk()

    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private val textEditSessionHolder = TextEditSessionHolder()

    private fun draft() = ToppingDraft(groupId = GroupId(1L), parfaitId = ParfaitId(2L), nextPositionZ = 1)

    private fun viewModel(isEditingExisting: Boolean = false): CanvasTextInputViewModel {
        every { getToppingDraftFlow() } returns flowOf(draft())
        every { getTodayParfaitFlowUseCase(any(), any()) } returns flowOf<CanvasVO?>(null)
        return CanvasTextInputViewModel(
            isEditingExisting = isEditingExisting,
            textEditSessionHolder = textEditSessionHolder,
            getToppingDraftFlow = getToppingDraftFlow,
            getTodayParfaitFlowUseCase = getTodayParfaitFlowUseCase,
            clearToppingDraft = clearToppingDraft,
            placeTextTopping = placeTextTopping,
            refreshTodayParfaitDetail = refreshTodayParfaitDetail,
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

    private fun placed() = PlacedTextToppingVO(
        parfaitTextId = ParfaitTextId(9L),
        text = "hi",
        transform = ToppingTransform(0.5, 0.5, 1, 1.0, 0.0),
        style = TextToppingStyle(color = "#FFFFFF", fontSize = 24.0, isFill = false, isStroke = false),
        placedBy = mockk(),
    )

    private fun TestScope.stackReadyViewModel(): CanvasTextInputViewModel {
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.success(placed())
        coEvery { refreshTodayParfaitDetail(any(), any()) } returns Result.success(Unit)
        val viewModel = viewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("hi"))
        viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(200.dp, 400.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(100.dp, 40.dp)))
        // 초안 구독이 돌아야 쌓기가 식별값을 읽는다
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun onClickStack_success_placesRefreshesClearsAndNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        val state = viewModel.state.value

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
        coVerifyOrder {
            placeTextTopping(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(2L),
                text = "hi",
                transform = toTextToppingTransform(
                    offsetX = state.offsetX,
                    offsetY = state.offsetY,
                    boxSize = DpSize(100.dp, 40.dp),
                    canvasSize = DpSize(200.dp, 400.dp),
                    scale = state.scale,
                    rotationDegrees = state.rotationDegrees,
                    positionZ = 1,
                ),
                style = textToppingStyleOf(state.selectedColor, state.textType, state.designFontSize),
            )
            refreshTodayParfaitDetail(GroupId(1L), ParfaitId(2L))
            clearToppingDraft()
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onClickStack_whileInFlight_placesOnlyOnce() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        val gate = CompletableDeferred<Unit>()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } coAnswers {
            gate.await()
            Result.success(placed())
        }

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)
        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { placeTextTopping(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickStack_permanentFailure_keepsContentAndDraftWithoutRetry() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Server(code = "PARFAIT_ALREADY_CLOSED", statusCode = null, serverMessage = "닫힘"),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.ShowPlaceFailed(PlaceFailure.Permanent), awaitItem())
        }
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("hi", viewModel.state.value.content)
        coVerify(exactly = 0) { clearToppingDraft() }
        coVerify(exactly = 1) { placeTextTopping(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickStack_networkFailure_showsUncertainAndRefreshesCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Network(IOException("connection reset")),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.ShowPlaceFailed(PlaceFailure.Uncertain), awaitItem())
        }
        assertEquals("hi", viewModel.state.value.content)
        coVerify(exactly = 1) { refreshTodayParfaitDetail(GroupId(1L), ParfaitId(2L)) }
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun onClickStack_networkFailure_stillNotifiesWhenRefreshFails() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Network(IOException("connection reset")),
        )
        coEvery { refreshTodayParfaitDetail(any(), any()) } returns Result.failure(
            AppError.Network(IOException("still down")),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.ShowPlaceFailed(PlaceFailure.Uncertain), awaitItem())
        }
    }

    @Test
    fun onClickStack_serverFailure_showsRetryableWithoutRefresh() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Server(code = "INTERNAL_ERROR", statusCode = 500, serverMessage = "오류"),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.ShowPlaceFailed(PlaceFailure.Retryable), awaitItem())
        }
        coVerify(exactly = 0) { refreshTodayParfaitDetail(any(), any()) }
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun onClickStack_withoutMeasurements_doesNotPostButStillLogsAnalytics() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("hi"))

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()

        coVerify(exactly = 0) { placeTextTopping(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { analyticsLogger.logEvent(any()) }
    }

    @Test
    fun onClickStack_logsAnalyticsOnEveryTapRegardlessOfResult() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = stackReadyViewModel()
        coEvery { placeTextTopping(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Network(IOException("connection reset")),
        )

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()
        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()

        verify(exactly = 2) {
            analyticsLogger.logEvent(match { it.type == AnalyticsEventType.CANVAS_TEXT_STACK_TAPPED })
        }
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

    // 쌓인 텍스트 다시 편집

    private fun original() = EditableText(
        parfaitTextId = 42L,
        isMine = true,
        text = "원래 글",
        color = TextToppingColor.Black,
        type = TextToppingType.FILL,
        designFontSize = 18f,
        positionX = 0.3f,
        positionY = 0.6f,
        positionZ = 7,
        scale = 1.5f,
        rotationDegrees = 20f,
    )

    private fun TestScope.editingViewModel(): CanvasTextInputViewModel {
        textEditSessionHolder.start(TextEditSession(groupId = 5L, parfaitId = 6L, original = original()))
        val viewModel = viewModel(isEditingExisting = true)
        advanceUntilIdle()
        return viewModel
    }

    private fun CanvasTextInputViewModel.measure() {
        processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(200.dp, 400.dp)))
        processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(100.dp, 40.dp)))
    }

    @Test
    fun editMode_seedsContentAndStyleFromTheSession() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()

        val state = viewModel.state.value
        assertEquals("원래 글", state.content)
        assertEquals(TextToppingColor.Black, state.selectedColor)
        assertEquals(TextToppingType.FILL, state.textType)
        assertEquals(18f, state.designFontSize)
        assertEquals(1.5f, state.scale)
        assertEquals(20f, state.rotationDegrees)
        assertFalse(state.hasUserAdjustedPlacement)
    }

    @Test
    fun editMode_restoresOffsetFromTheCenterRatioOnceMeasured() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()

        viewModel.measure()

        // 중심 (60, 240) 에서 박스 절반을 뺀 좌상단
        val state = viewModel.state.value
        assertEquals(10f, state.offsetX.value, 1e-4f)
        assertEquals(220f, state.offsetY.value, 1e-4f)
    }

    @Test
    fun editMode_boxRemeasuredWithNewSize_keepsTheOriginalCenter() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()
        // 첫 레이아웃은 키보드 높이가 0 이라 박스가 거의 0 으로 재진다
        viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(DpSize(200.dp, 400.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(1.dp, 1.dp)))
        viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(DpSize(120.dp, 50.dp)))

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()

        val edited = textEditSessionHolder.result.value!!.second as TextEditResult.Edited
        assertEquals(0.3f, edited.positionX, 1e-5f)
        assertEquals(0.6f, edited.positionY, 1e-5f)
    }

    @Test
    fun editMode_observesTheSessionCanvasNotTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        textEditSessionHolder.start(TextEditSession(groupId = 5L, parfaitId = 6L, original = original()))
        val viewModel = viewModel(isEditingExisting = true)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        verify { getTodayParfaitFlowUseCase(GroupId(5L), any()) }
        verify(exactly = 0) { getToppingDraftFlow() }
    }

    @Test
    fun editMode_stackWithoutChanges_roundTripsTheSameValuesWithoutServerCall() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = editingViewModel()
            viewModel.measure()

            viewModel.effect.test {
                viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
                advanceUntilIdle()

                assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
            }

            val (id, result) = textEditSessionHolder.result.value!!
            assertEquals(42L, id)
            val edited = result as TextEditResult.Edited
            assertEquals("원래 글", edited.text)
            assertEquals(TextToppingColor.Black, edited.color)
            assertEquals(TextToppingType.FILL, edited.type)
            assertEquals(18f, edited.designFontSize)
            assertEquals(0.3f, edited.positionX, 1e-5f)
            assertEquals(0.6f, edited.positionY, 1e-5f)
            assertEquals(1.5f, edited.scale)
            assertEquals(20f, edited.rotationDegrees)
            coVerify(exactly = 0) { placeTextTopping(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { clearToppingDraft() }
            verify(exactly = 0) { analyticsLogger.logEvent(any()) }
        }

    @Test
    fun editMode_stackAfterEditing_returnsTheEditedValues() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()
        viewModel.measure()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange("새 글"))
        viewModel.processIntent(CanvasTextInputIntent.OnSelectColor(TextToppingColor.White))
        viewModel.processIntent(
            CanvasTextInputIntent.OnToppingTransform(pan = DpOffset(20.dp, 0.dp), zoom = 1f, rotationDelta = 0f),
        )

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()

        val edited = textEditSessionHolder.result.value!!.second as TextEditResult.Edited
        assertEquals("새 글", edited.text)
        assertEquals(TextToppingColor.White, edited.color)
        assertEquals(0.4f, edited.positionX, 1e-5f)
    }

    @Test
    fun editMode_confirmWithBlankContent_finishesAsDeleted() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnContentChange(""))

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
        assertEquals(42L to TextEditResult.Deleted, textEditSessionHolder.result.value)
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun editMode_quitFromHeader_leavesWithoutResult() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = editingViewModel()
        viewModel.processIntent(CanvasTextInputIntent.OnClickClose)

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogConfirm)
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
        assertNull(textEditSessionHolder.result.value)
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun editMode_withoutSession_navigatesBackImmediately() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(isEditingExisting = true)

        viewModel.effect.test {
            advanceUntilIdle()

            assertEquals(CanvasTextInputEffect.NavigateBack, awaitItem())
        }
    }
}
