package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteTextToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.PlaceTextToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateTextToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableText
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditResult
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditSession
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingColor
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextToppingType
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.util.TextEditSessionHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.toWireColor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GROUP_ID = 1L
private const val PARFAIT_ID = 100L

private const val FIRST_ID = 1L
private const val SECOND_ID = 2L
private const val OTHERS_ID = 3L
private const val SHARED_ID = 5L
private const val OTHERS_TEXT_ID = 6L
private const val MY_SECOND_TEXT_ID = 7L

private const val MY_MEMBER_ID = 11L
private const val OTHER_MEMBER_ID = 22L

private val BLACK_ARGB = 0xFF000000.toInt()
private val ORANGE_ARGB = 0xFFFF6B00.toInt()

class CanvasToppingArrangeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTodayParfaitFlow: GetTodayParfaitFlowUseCase = mockk()
    private val refreshTodayParfaitDetail: RefreshTodayParfaitDetailUseCase = mockk()
    private val deleteTopping: DeleteToppingUseCase = mockk()
    private val deleteTextTopping: DeleteTextToppingUseCase = mockk()
    private val updateToppings: UpdateToppingsUseCase = mockk()
    private val updateToppingBorder: UpdateToppingBorderUseCase = mockk()
    private val placeTextTopping: PlaceTextToppingUseCase = mockk()
    private val updateTextTopping: UpdateTextToppingUseCase = mockk()
    private val textEditSessionHolder = TextEditSessionHolder()
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    /** 저장소의 오늘 캔버스 캐시. 갱신이 성공했다는 것은 여기에 값이 실린다는 뜻이다 */
    private val todayCanvases = MutableStateFlow<CanvasVO?>(null)

    private fun stubTheHappyPath(initialCanvas: CanvasVO) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfaitDetail(any(), any()) } returns Result.success(Unit)
        todayCanvases.value = initialCanvas
    }

    /**
     * `launchWhileSubscribed` 는 [CanvasToppingArrangeViewModel.state] 의 구독자 수로 폴링 수명을
     * 잰다 — 라우트의 `collectAsStateWithLifecycle()` 을 흉내 내 여기서 먼저 구독을 붙여야
     * 오늘 캔버스 구독이 열리고 토핑이 시딩된다.
     */
    private fun TestScope.viewModel(
        initialToppingId: Long = FIRST_ID,
        initialCanvas: CanvasVO = canvas(),
        initialIsText: Boolean = false,
    ): CanvasToppingArrangeViewModel {
        stubTheHappyPath(initialCanvas)
        return CanvasToppingArrangeViewModel(
            groupIdValue = GROUP_ID,
            parfaitIdValue = PARFAIT_ID,
            initialToppingId = initialToppingId,
            initialIsText = initialIsText,
            getTodayParfaitFlowUseCase = getTodayParfaitFlow,
            refreshTodayParfaitDetailUseCase = refreshTodayParfaitDetail,
            deleteToppingUseCase = deleteTopping,
            deleteTextToppingUseCase = deleteTextTopping,
            updateToppingsUseCase = updateToppings,
            updateToppingBorderUseCase = updateToppingBorder,
            placeTextToppingUseCase = placeTextTopping,
            updateTextToppingUseCase = updateTextTopping,
            textEditSessionHolder = textEditSessionHolder,
            analyticsLogger = analyticsLogger,
        ).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()
        }
    }

    private fun CanvasToppingArrangeViewModel.topping(id: Long): EditableTopping =
        state.value.toppings.first { it.parfaitImageId == id }

    private fun CanvasToppingArrangeViewModel.click(id: Long) =
        processIntent(CanvasToppingArrangeIntent.OnClickTopping(topping(id)))

    private fun CanvasToppingArrangeViewModel.drag(panX: Float = 0.1f) = processIntent(
        CanvasToppingArrangeIntent.OnToppingTransform(panX = panX, panY = 0f, zoom = 1f, rotationDelta = 0f),
    )

    private fun stubDelete(
        id: Long,
        result: Result<Unit> = Result.success(Unit),
    ) {
        coEvery { deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(id)) } returns result
    }

    /** @return 완료시키기 전까지 갱신이 끝나지 않게 붙드는 문 */
    private fun holdTheRefresh(): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshTodayParfaitDetail(any(), any()) } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        return gate
    }

    private fun stubTransformSaveSucceeds() {
        coEvery { updateToppings(any(), any(), any()) } returns Result.success(emptyList())
    }

    private fun stubBorderSave(
        id: Long,
        border: ToppingBorder,
        result: Result<UpdatedToppingBorderVO> = Result.success(UpdatedToppingBorderVO(ParfaitImageId(id), border)),
    ) {
        coEvery {
            updateToppingBorder(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(id), border)
        } returns result
    }

    // 진입·변환

    @Test
    fun firstEmission_focusesInitialTopping() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(initialToppingId = FIRST_ID)

        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun init_placesToppingsByTheStoredRatiosAndMarksMine() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        val toppings = viewModel.state.value.toppings
        assertEquals(listOf(FIRST_ID, SECOND_ID, OTHERS_ID), toppings.map(EditableTopping::parfaitImageId))
        assertEquals(listOf(true, true, false), toppings.map(EditableTopping::isMine))
        assertEquals(0.25f, toppings.first().positionX)
        assertEquals(0.75f, toppings.first().positionY)
    }

    @Test
    fun init_ordersToppingsByPositionZ() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        // 뒤에 그려야 할 토핑이 목록 앞쪽에 온다
        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID, positionZ = 5),
                toppingVO(SECOND_ID, positionZ = 1),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(SECOND_ID, FIRST_ID),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
    }

    @Test
    fun init_solidBorder_becomesBorderStyle() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "#FF6B00", width = 4.0))),
        )
        advanceUntilIdle()

        assertEquals(ToppingBorderStyle(colorArgb = ORANGE_ARGB, widthDp = 4f), viewModel.topping(FIRST_ID).border)
    }

    @Test
    fun init_unreadableBorderColor_hasNoBorder() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "무지개", width = 4.0))),
        )
        advanceUntilIdle()

        // 임의의 색을 두르는 대신 테두리를 만들지 않는다
        assertNull(viewModel.topping(FIRST_ID).border)
    }

    @Test
    fun firstEmission_withDifferentParfaitId_movesTheEditTarget() = runTest(mainDispatcherRule.dispatcher) {
        // Given 연 parfaitId 는 100 인데 최초 방출은 9 다
        val viewModel = viewModel(initialCanvas = canvas(parfaitId = 9L))
        viewModel.drag()
        coEvery { updateToppings(GroupId(GROUP_ID), ParfaitId(9L), any()) } returns Result.success(emptyList())

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) { updateToppings(GroupId(GROUP_ID), ParfaitId(9L), any()) }
    }

    // 탭 네 갈래 (패널이 닫힌 상태에서)

    @Test
    fun clickFocusedTopping_opensPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.click(FIRST_ID)

        assertTrue(viewModel.state.value.isBorderPanelOpen)
    }

    @Test
    fun clickOtherOwnTopping_movesFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.click(SECOND_ID)

        assertEquals(CanvasToppingKey.Image(SECOND_ID), viewModel.state.value.focusedKey)
        assertFalse(viewModel.state.value.isBorderPanelOpen)
    }

    @Test
    fun clickOthersTopping_showsToastAndKeepsFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.click(OTHERS_ID)

            assertEquals(CanvasToppingArrangeEffect.ShowOthersToppingNotEditable, awaitItem())
        }
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun clickEmptyCanvas_clearsFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)

        assertNull(viewModel.state.value.focusedKey)
    }

    // 패널

    @Test
    fun togglePanel_withoutFocus_staysClosed() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertFalse(viewModel.state.value.canOpenBorderPanel)
    }

    @Test
    fun togglePanel_withFocus_opensThenCloses() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)
        assertTrue(viewModel.state.value.isBorderPanelOpen)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)
        assertFalse(viewModel.state.value.isBorderPanelOpen)
    }

    @Test
    fun dismissPanel_closesAnOpenPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)
        assertTrue(viewModel.state.value.isBorderPanelOpen)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnDismissBorderPanel)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun panelValues_followFocusedTopping() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "#000000", width = 8.0)),
                toppingVO(SECOND_ID),
            ),
        )
        advanceUntilIdle()

        assertEquals(BLACK_ARGB, viewModel.state.value.panelBorderColorArgb)
        assertEquals(8f, viewModel.state.value.panelBorderWidthDp)

        viewModel.click(SECOND_ID)

        assertNull(viewModel.state.value.panelBorderColorArgb)
        assertEquals(10f, viewModel.state.value.panelBorderWidthDp)
    }

    @Test
    fun changeWidth_withoutBorder_keepsPendingAcrossFocusChange() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(16f))
        viewModel.click(FIRST_ID)
        viewModel.click(SECOND_ID)

        assertEquals(16f, viewModel.state.value.panelBorderWidthDp)
        // 테두리가 없는 토핑의 굵기는 값이 아니라 대기 중인 선택이라 손댄 것으로 치지 않는다
        assertFalse(SECOND_ID in viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun selectColor_setsBorderOnFocusedToppingAndMarksDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))

        assertEquals(ToppingBorderStyle(ORANGE_ARGB, 10f), viewModel.topping(FIRST_ID).border)
        assertTrue(FIRST_ID in viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun selectNone_removesBorderAndMarksDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))

        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(null))

        assertNull(viewModel.topping(FIRST_ID).border)
        assertTrue(FIRST_ID in viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun changeWidth_withBorder_marksDirtyAndUpdatesPending() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "#000000", width = 8.0)),
                toppingVO(SECOND_ID),
            ),
        )
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(12f))

        assertEquals(12f, viewModel.topping(FIRST_ID).border?.widthDp)
        assertTrue(FIRST_ID in viewModel.state.value.dirtyToppingIds)
        assertEquals(12f, viewModel.state.value.pendingBorderWidthDp)
    }

    @Test
    fun changeWidth_isClampedToRange() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(10_000f))
        assertEquals(TOPPING_BORDER_WIDTH_RANGE_DP.endInclusive, viewModel.state.value.pendingBorderWidthDp)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(-10_000f))
        assertEquals(TOPPING_BORDER_WIDTH_RANGE_DP.start, viewModel.state.value.pendingBorderWidthDp)
    }

    // 패널이 열린 동안

    @Test
    fun transform_whilePanelOpen_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)
        val before = viewModel.topping(FIRST_ID)

        viewModel.drag()

        assertEquals(before, viewModel.topping(FIRST_ID))
        assertTrue(
            viewModel.state.value.dirtyToppingIds
                .isEmpty(),
        )
    }

    @Test
    fun clickOtherOwnTopping_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.click(SECOND_ID)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun clickOthersTopping_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.effect.test {
            viewModel.click(OTHERS_ID)

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun clickEmptyCanvas_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun clickDeleteButton_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(CanvasToppingKey.Image(FIRST_ID), viewModel.state.value.focusedKey)
        assertTrue(
            viewModel.state.value.pendingDeleteToppingIds
                .isEmpty(),
        )
    }

    @Test
    fun focusedToppingRemovedByPolling_closesPanel_andSendsNoPatch() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(BLACK_ARGB))
        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID), toppingVO(OTHERS_ID, isMine = false)))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertNull(viewModel.state.value.focusedKey)
        assertFalse(FIRST_ID in viewModel.state.value.dirtyToppingIds)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 0) { updateToppingBorder(any(), any(), ParfaitImageId(FIRST_ID), any()) }
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
    }

    // 변형

    @Test
    fun transform_appliesToFocusedToppingAndMarksDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(
            CanvasToppingArrangeIntent.OnToppingTransform(panX = 0.1f, panY = 0.05f, zoom = 2f, rotationDelta = 30f),
        )

        val moved = viewModel.topping(FIRST_ID)
        assertEquals(0.35f, moved.positionX)
        assertEquals(0.80f, moved.positionY)
        assertEquals(2f, moved.scale)
        assertEquals(30f, moved.rotationDegrees)
        assertEquals(setOf(FIRST_ID), viewModel.state.value.dirtyToppingIds)
        // 포커스 아닌 토핑은 그대로다
        assertEquals(0.25f, viewModel.topping(SECOND_ID).positionX)
    }

    @Test
    fun transform_withoutFocus_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)
        val before = viewModel.state.value

        viewModel.drag()

        assertEquals(before.toppings, viewModel.state.value.toppings)
        assertEquals(before.dirtyToppingIds, viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun transform_clampsAtMinScale() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(
            CanvasToppingArrangeIntent.OnToppingTransform(panX = 0f, panY = 0f, zoom = 0f, rotationDelta = 0f),
        )

        assertEquals(TOPPING_MIN_SCALE, viewModel.topping(FIRST_ID).scale)
    }

    // 닫기

    @Test
    fun clickClose_withoutChanges_navigatesBackWithoutQuitDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun clickClose_withChanges_showsQuitDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

            expectNoEvents()
        }
        assertTrue(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun clickClose_whileLoading_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        val pending = CompletableDeferred<Result<List<UpdatedToppingVO>>>()
        val viewModel = viewModel()
        viewModel.drag()
        coEvery { updateToppings(any(), any(), any()) } coAnswers { pending.await() }
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

            expectNoEvents()
        }
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun systemBack_withPanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun systemBack_withChanges_navigatesBackWithoutQuitDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun systemBack_whileLoading_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        val pending = CompletableDeferred<Result<List<UpdatedToppingVO>>>()
        val viewModel = viewModel()
        viewModel.drag()
        coEvery { updateToppings(any(), any(), any()) } coAnswers { pending.await() }
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack)

            expectNoEvents()
        }
    }

    @Test
    fun systemBack_whileRefreshingAfterDelete_isIgnored() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID)
        val refreshGate = holdTheRefresh()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack)

            // 여기서 되감으면 확정이 낼 되감기와 겹친다
            expectNoEvents()
        }
        refreshGate.complete(Unit)
    }

    @Test
    fun quitDialogConfirm_closesDialogAndEmitsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)
        assertTrue(viewModel.state.value.showQuitDialog)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    // 확정

    @Test
    fun confirm_sendsOnlyChangedAxes() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        val updates = slot<List<ToppingTransformUpdate>>()
        coEvery { updateToppings(any(), any(), capture(updates)) } returns Result.success(emptyList())
        stubBorderSave(SECOND_ID, ToppingBorder.Solid(color = "#FF6B00", width = 10.0))

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()

        assertEquals(listOf(ParfaitImageId(FIRST_ID)), updates.captured.map { it.parfaitImageId })
        coVerify(exactly = 1) { updateToppingBorder(any(), any(), any(), any()) }
        coVerify(exactly = 1) {
            updateToppingBorder(
                GroupId(GROUP_ID),
                ParfaitId(PARFAIT_ID),
                ParfaitImageId(SECOND_ID),
                ToppingBorder.Solid(color = "#FF6B00", width = 10.0),
            )
        }
    }

    @Test
    fun confirm_colorChangedAndReverted_sendsNoBorderPatch() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "#000000", width = 8.0)),
                toppingVO(SECOND_ID),
            ),
        )
        advanceUntilIdle()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(BLACK_ARGB))
        assertTrue(FIRST_ID in viewModel.state.value.dirtyToppingIds)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        // Then dirty 로 남아 있어도 서버 값과 같으니 보낼 것이 없다
        coVerify(exactly = 0) { updateToppingBorder(any(), any(), any(), any()) }
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
    }

    @Test
    fun confirm_widthChangedOnOffPaletteColor_patchesSameColorNewWidth() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID, border = ToppingBorder.Solid(color = "#123456", width = 8.0)),
                toppingVO(SECOND_ID),
            ),
        )
        advanceUntilIdle()
        viewModel.click(FIRST_ID)
        stubBorderSave(FIRST_ID, ToppingBorder.Solid(color = "#123456", width = 12.0))

        viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(12f))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updateToppingBorder(
                GroupId(GROUP_ID),
                ParfaitId(PARFAIT_ID),
                ParfaitImageId(FIRST_ID),
                ToppingBorder.Solid(color = "#123456", width = 12.0),
            )
        }
    }

    @Test
    fun confirm_whilePanelOpen_sendsStoredPosition() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        viewModel.click(FIRST_ID)
        assertTrue(viewModel.state.value.isBorderPanelOpen)
        val updates = slot<List<ToppingTransformUpdate>>()
        coEvery { updateToppings(any(), any(), capture(updates)) } returns Result.success(emptyList())

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()

        assertEquals(0.35f.toDouble(), updates.captured.single().positionX)
    }

    @Test
    fun confirm_showsLoadingUntilDone() = runTest(mainDispatcherRule.dispatcher) {
        val pending = CompletableDeferred<Result<List<UpdatedToppingVO>>>()
        val viewModel = viewModel()
        viewModel.drag()
        coEvery { updateToppings(any(), any(), any()) } coAnswers { pending.await() }

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isLoading)

        pending.complete(Result.success(emptyList()))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun confirm_success_refreshesThenNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        stubTransformSaveSucceeds()
        val refreshGate = holdTheRefresh()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            // 갱신이 도는 동안에는 되감지 않고 덮개도 걷지 않는다
            expectNoEvents()
            assertTrue(viewModel.state.value.isLoading)

            refreshGate.complete(Unit)
            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        assertFalse(viewModel.state.value.isLoading)

        // 되감기 전에 갱신을 마쳐야 캔버스가 저장된 값을 본다
        coVerifyOrder {
            updateToppings(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), any())
            refreshTodayParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID))
        }
        assertTrue(
            viewModel.state.value.dirtyToppingIds
                .isEmpty(),
        )
    }

    @Test
    fun confirm_nothingDirty_refreshesAndNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { refreshTodayParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID)) }
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
        coVerify(exactly = 0) { updateToppingBorder(any(), any(), any(), any()) }
    }

    @Test
    fun confirm_borderPatchFails_staysAndKeepsOnlyFailedDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        stubTransformSaveSucceeds()
        stubBorderSave(
            id = SECOND_ID,
            border = ToppingBorder.Solid(color = "#FF6B00", width = 10.0),
            result = Result.failure(RuntimeException("실패")),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }
        // 저장된 토핑 1 은 빠지고 실패한 토핑 2 만 남아 다시 누르면 그것만 보낸다
        assertEquals(setOf(SECOND_ID), viewModel.state.value.dirtyToppingIds)
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { refreshTodayParfaitDetail(any(), any()) }
    }

    @Test
    fun confirm_failure_keepsPanelOpen() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        stubBorderSave(
            id = FIRST_ID,
            border = ToppingBorder.Solid(color = "#FF6B00", width = 10.0),
            result = Result.failure(AppError.Network(null)),
        )

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
        }
        assertTrue(viewModel.state.value.isBorderPanelOpen)
    }

    @Test
    fun confirm_unexpectedException_showsSaveError() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        coEvery { updateToppings(any(), any(), any()) } throws IllegalStateException("예상 밖")

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    // 삭제

    @Test
    fun clickDelete_removesTheToppingOnScreenOnly() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

            expectNoEvents()
        }

        assertEquals(
            listOf(SECOND_ID, OTHERS_ID),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
        assertNull(viewModel.state.value.focusedKey)
        assertFalse(viewModel.state.value.canOpenBorderPanel)
        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
    }

    @Test
    fun clickDelete_keepsOtherToppingsChanges() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        val moved = viewModel.topping(FIRST_ID).positionX
        viewModel.click(SECOND_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        assertEquals(moved, viewModel.topping(FIRST_ID).positionX)
        assertEquals(setOf(FIRST_ID), viewModel.state.value.dirtyToppingIds)
    }

    @Test
    fun clickDelete_thenClickOtherOwnTopping_focusesIt() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.click(SECOND_ID)

        assertEquals(CanvasToppingKey.Image(SECOND_ID), viewModel.state.value.focusedKey)
        assertTrue(viewModel.state.value.canOpenBorderPanel)
    }

    @Test
    fun clickClose_withOnlyADelete_showsQuitDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

            expectNoEvents()
        }
        assertTrue(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun quitDialogConfirm_afterDelete_leavesWithoutDeleting() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
    }

    @Test
    fun confirm_afterDelete_deletesThenRefreshesThenNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        val refreshGate = holdTheRefresh()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            // 갱신이 도는 동안에는 되감지 않고 덮개도 걷지 않는다
            expectNoEvents()
            assertTrue(viewModel.state.value.isLoading)

            refreshGate.complete(Unit)
            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerifyOrder {
            deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(FIRST_ID))
            refreshTodayParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID))
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun confirm_afterDelete_sendsNoPatchForTheDeletedTopping() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID)
        viewModel.drag()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
        coVerify(exactly = 0) { updateToppingBorder(any(), any(), any(), any()) }
    }

    @Test
    fun confirm_savesOtherChangesBeforeDeleting() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubTransformSaveSucceeds()
        stubDelete(SECOND_ID)
        viewModel.drag()
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerifyOrder {
            updateToppings(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), any())
            deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(SECOND_ID))
        }
    }

    @Test
    fun confirm_saveFails_doesNotDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery { updateToppings(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))
        viewModel.drag()
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }

        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
        assertEquals(setOf(SECOND_ID), viewModel.state.value.pendingDeleteToppingIds)
    }

    @Test
    fun confirm_deleteFails_showsErrorAndKeepsItPending() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID, result = Result.failure(RuntimeException("실패")))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }

        assertEquals(setOf(FIRST_ID), viewModel.state.value.pendingDeleteToppingIds)
        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == FIRST_ID },
        )
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { refreshTodayParfaitDetail(any(), any()) }
    }

    @Test
    fun confirm_deleteOfAnAlreadyGoneTopping_countsAsDeleted() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(
            id = FIRST_ID,
            result = Result.failure(
                AppError.Server(code = "PARFAIT_IMAGE_NOT_FOUND", statusCode = 404, serverMessage = "없음"),
            ),
        )
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun confirm_deleteRejectedForAnotherReason_isAFailure() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(
            id = FIRST_ID,
            result = Result.failure(
                AppError.Server(code = "PARFAIT_ALREADY_CLOSED", statusCode = 409, serverMessage = "마감"),
            ),
        )
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun confirm_oneOfTwoDeletesFails_showsErrorAndKeepsBothOffScreen() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID)
        stubDelete(SECOND_ID, result = Result.failure(RuntimeException("실패")))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }

        // 서버에서 이미 지워진 토핑이 뒤늦은 응답으로 되살아나지 않는다
        todayCanvases.value = canvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()

        assertEquals(
            listOf(OTHERS_ID),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
    }

    @Test
    fun confirmAgain_afterAPartialDeleteFailure_resendsOnlyTheFailedOne() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        stubDelete(FIRST_ID)
        stubDelete(SECOND_ID, result = Result.failure(RuntimeException("실패")))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.click(SECOND_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            stubDelete(SECOND_ID)

            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { deleteTopping(any(), any(), ParfaitImageId(FIRST_ID)) }
        coVerify(exactly = 2) { deleteTopping(any(), any(), ParfaitImageId(SECOND_ID)) }
    }

    @Test
    fun confirm_refreshThrows_stillNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery { refreshTodayParfaitDetail(any(), any()) } throws RuntimeException("갱신 실패")

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            // 저장은 끝났다. 갱신이 터졌다고 저장 실패로 알리지 않는다
            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    // 병합

    @Test
    fun merge_dirtyTopping_isNotOverwritten() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag(panX = 0.2f)
        val moved = viewModel.topping(FIRST_ID).positionX

        todayCanvases.value = canvas(toppings = listOf(toppingVO(FIRST_ID, positionX = 0.9)))
        advanceUntilIdle()

        assertEquals(moved, viewModel.topping(FIRST_ID).positionX)
    }

    @Test
    fun merge_cleanTopping_takesTheServerValue() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        todayCanvases.value = canvas(toppings = listOf(toppingVO(FIRST_ID, positionX = 0.9)))
        advanceUntilIdle()

        assertEquals(0.9f, viewModel.topping(FIRST_ID).positionX)
    }

    @Test
    fun merge_newToppingFromAnotherMember_appears() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        assertEquals(3, viewModel.state.value.toppings.size)

        todayCanvases.value = canvas(
            toppings = listOf(
                toppingVO(FIRST_ID),
                toppingVO(SECOND_ID),
                toppingVO(OTHERS_ID, isMine = false),
                toppingVO(4L, isMine = false),
            ),
        )
        advanceUntilIdle()

        assertEquals(4, viewModel.state.value.toppings.size)
    }

    @Test
    fun merge_deletedTopping_doesNotComeBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        todayCanvases.value = canvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == FIRST_ID },
        )
    }

    @Test
    fun merge_whenTheServerDropsIt_clearsThePendingDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        assertEquals(setOf(FIRST_ID), viewModel.state.value.pendingDeleteToppingIds)

        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID)))
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.pendingDeleteToppingIds
                .isEmpty(),
        )
    }

    @Test
    fun merge_whenTheServerDropsIt_clearsTheDirtyMark() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.drag()
        assertTrue(FIRST_ID in viewModel.state.value.dirtyToppingIds)

        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID)))
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.dirtyToppingIds
                .isEmpty(),
        )
    }

    // 텍스트

    /** 이미지와 텍스트가 같은 id 5 를 갖는다 */
    private fun textCanvas() = canvas(
        toppings = listOf(toppingVO(SHARED_ID, positionZ = 1)),
        texts = listOf(
            textVO(SHARED_ID, positionZ = 2),
            textVO(OTHERS_TEXT_ID, isMine = false, positionZ = 3),
            textVO(MY_SECOND_TEXT_ID, positionZ = 4),
        ),
    )

    private fun TestScope.textViewModel(): CanvasToppingArrangeViewModel =
        viewModel(initialToppingId = SHARED_ID, initialCanvas = textCanvas(), initialIsText = true)

    private fun CanvasToppingArrangeViewModel.text(id: Long): EditableText =
        state.value.texts.first { it.parfaitTextId == id }

    @Test
    fun initialIsText_focusesTheTextNotTheImageWithTheSameId() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        assertEquals(CanvasToppingKey.Text(SHARED_ID), viewModel.state.value.focusedKey)
        assertNull(viewModel.state.value.focusedTopping)
    }

    @Test
    fun init_mapsTextsFromTheCanvas() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        val text = viewModel.text(SHARED_ID)
        assertEquals("안녕", text.text)
        assertEquals(18f, text.designFontSize)
        assertEquals(0.4f, text.positionX)
        assertEquals(0.6f, text.positionY)
        assertEquals(2, text.positionZ)
        assertEquals(30f, text.rotationDegrees)
    }

    @Test
    fun transform_onFocusedText_movesOnlyTheText() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        val imageBefore = viewModel.topping(SHARED_ID)

        viewModel.processIntent(
            CanvasToppingArrangeIntent.OnToppingTransform(panX = 0.1f, panY = 0.05f, zoom = 2f, rotationDelta = 10f),
        )

        val text = viewModel.text(SHARED_ID)
        assertEquals(0.5f, text.positionX, 1e-6f)
        assertEquals(0.65f, text.positionY, 1e-6f)
        assertEquals(2.4f, text.scale, 1e-6f)
        assertEquals(40f, text.rotationDegrees, 1e-6f)
        assertEquals(setOf(SHARED_ID), viewModel.state.value.dirtyTextIds)
        assertEquals(imageBefore, viewModel.topping(SHARED_ID))
        assertTrue(
            viewModel.state.value.dirtyToppingIds
                .isEmpty(),
        )
        assertTrue(viewModel.state.value.hasUnsavedChanges)
    }

    @Test
    fun clickMyText_movesFocusToIt() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel(initialToppingId = SHARED_ID, initialCanvas = textCanvas())

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(viewModel.text(SHARED_ID)))

        assertEquals(CanvasToppingKey.Text(SHARED_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun clickOthersText_showsToastAndKeepsFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(viewModel.text(OTHERS_TEXT_ID)))

            assertEquals(CanvasToppingArrangeEffect.ShowOthersToppingNotEditable, awaitItem())
        }
        assertEquals(CanvasToppingKey.Text(SHARED_ID), viewModel.state.value.focusedKey)
    }

    @Test
    fun togglePanel_whileTextFocused_staysClosed() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertFalse(viewModel.state.value.canOpenBorderPanel)
    }

    @Test
    fun deleteText_hidesItAndKeepsItHiddenAcrossEmissions() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        assertEquals(setOf(SHARED_ID), viewModel.state.value.pendingDeleteTextIds)
        assertTrue(
            viewModel.state.value.pendingDeleteToppingIds
                .isEmpty(),
        )
        assertTrue(
            viewModel.state.value.texts
                .none { it.parfaitTextId == SHARED_ID },
        )
        assertEquals(
            listOf(SHARED_ID),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
        assertNull(viewModel.state.value.focusedKey)

        todayCanvases.value = textCanvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.texts
                .none { it.parfaitTextId == SHARED_ID },
        )
        assertEquals(setOf(SHARED_ID), viewModel.state.value.pendingDeleteTextIds)
    }

    @Test
    fun dirtyText_keepsLocalValuesAcrossEmissions() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        viewModel.drag(panX = 0.1f)
        val moved = viewModel.text(SHARED_ID)

        // 폴링이 서버 값(positionX 0.4)을 다시 준다
        todayCanvases.value = textCanvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()

        assertEquals(moved, viewModel.text(SHARED_ID))
        assertEquals(0.5f, viewModel.text(SHARED_ID).positionX, 1e-6f)
        assertEquals(setOf(SHARED_ID), viewModel.state.value.dirtyTextIds)
    }

    @Test
    fun dirtyFocusedText_vanishedFromServer_isDroppedAndLosesFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        viewModel.drag()
        assertEquals(setOf(SHARED_ID), viewModel.state.value.dirtyTextIds)

        // 다른 기기에서 지워져 서버 목록에서 빠진다
        todayCanvases.value = textCanvas().copy(
            texts = textCanvas().texts.filterNot { it.parfaitTextId == ParfaitTextId(SHARED_ID) },
        )
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.texts
                .none { it.parfaitTextId == SHARED_ID },
        )
        assertTrue(
            viewModel.state.value.dirtyTextIds
                .isEmpty(),
        )
        assertNull(viewModel.state.value.focusedKey)
    }

    @Test
    fun confirm_deleteOfAnAlreadyGoneText_countsAsDeleted() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        coEvery { updateToppings(any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery { deleteTextTopping(any(), any(), any()) } returns Result.failure(
            AppError.Server(code = "PARFAIT_TEXT_NOT_FOUND", statusCode = 404, serverMessage = "없음"),
        )
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun deletedTexts_vanishedFromServer_areDroppedFromDeleteSets() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(viewModel.text(MY_SECOND_TEXT_ID)))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        coEvery { updateToppings(any(), any(), any(), any()) } returns Result.success(emptyList())
        coEvery {
            deleteTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitTextId(SHARED_ID))
        } returns Result.success(Unit)
        coEvery {
            deleteTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitTextId(MY_SECOND_TEXT_ID))
        } returns Result.failure(RuntimeException("실패"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
        }
        assertEquals(setOf(SHARED_ID), viewModel.state.value.deletedTextIds)
        assertEquals(setOf(MY_SECOND_TEXT_ID), viewModel.state.value.pendingDeleteTextIds)

        // 둘 다 서버 목록에서 빠진다
        todayCanvases.value = textCanvas().copy(
            texts = listOf(textVO(OTHERS_TEXT_ID, isMine = false, positionZ = 3)),
        )
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.deletedTextIds
                .isEmpty(),
        )
        assertTrue(
            viewModel.state.value.pendingDeleteTextIds
                .isEmpty(),
        )
    }

    @Test
    fun confirm_sendsImageAndTextTransformsInOneBatchThenDeletesText() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        viewModel.drag(panX = 0.1f)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickTopping(viewModel.topping(SHARED_ID)))
        viewModel.drag(panX = 0.2f)
        viewModel.processIntent(
            CanvasToppingArrangeIntent.OnClickText(viewModel.text(MY_SECOND_TEXT_ID)),
        )
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        val updates = slot<List<ToppingTransformUpdate>>()
        val textUpdates = slot<List<TextToppingTransformUpdate>>()
        coEvery {
            updateToppings(any(), any(), capture(updates), capture(textUpdates))
        } returns Result.success(emptyList())
        coEvery { deleteTextTopping(any(), any(), any()) } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { updateToppings(any(), any(), any(), any()) }
        assertEquals(listOf(ParfaitImageId(SHARED_ID)), updates.captured.map { it.parfaitImageId })
        // 텍스트는 바뀐 축만 싣는다 — 나머지 축은 null 이라 서버가 유지한다
        val textUpdate = textUpdates.captured.single()
        assertEquals(ParfaitTextId(SHARED_ID), textUpdate.parfaitTextId)
        assertEquals(0.5, textUpdate.positionX!!, 1e-6)
        assertNull(textUpdate.positionY)
        assertNull(textUpdate.positionZ)
        assertNull(textUpdate.scale)
        assertNull(textUpdate.rotation)
        coVerifyOrder {
            updateToppings(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), any(), any())
            deleteTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitTextId(MY_SECOND_TEXT_ID))
            refreshTodayParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID))
        }
        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
    }

    @Test
    fun confirm_batchFails_sendsNoTextDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickTopping(viewModel.topping(SHARED_ID)))
        viewModel.drag()
        coEvery { updateToppings(any(), any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }

        coVerify(exactly = 0) { deleteTextTopping(any(), any(), any()) }
        assertEquals(setOf(SHARED_ID), viewModel.state.value.pendingDeleteTextIds)
    }

    // 텍스트 다시 편집

    private fun edited(
        text: String = "안녕",
        color: TextToppingColor = TextToppingColor.Black,
    ) = TextEditResult.Edited(
        text = text,
        color = color,
        type = TextToppingType.DEFAULT,
        designFontSize = 18f,
        positionX = 0.4f,
        positionY = 0.6f,
        scale = 1.2f,
        rotationDegrees = 30f,
    )

    /** C-106 이 결과를 남기고 돌아온 상황을 흉내 낸다 */
    private fun TestScope.returnFromTextEdit(
        textId: Long,
        result: TextEditResult,
        viewModel: CanvasToppingArrangeViewModel,
    ) {
        textEditSessionHolder.start(
            TextEditSession(groupId = GROUP_ID, parfaitId = PARFAIT_ID, original = viewModel.text(textId)),
        )
        textEditSessionHolder.finish(result)
        advanceUntilIdle()
    }

    @Test
    fun tapFocusedMyText_startsEditSessionAndNavigates() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        val original = viewModel.text(SHARED_ID)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(original))

            assertEquals(CanvasToppingArrangeEffect.NavigateToTextEdit, awaitItem())
        }
        assertEquals(
            TextEditSession(groupId = GROUP_ID, parfaitId = PARFAIT_ID, original = original),
            textEditSessionHolder.session(),
        )
    }

    @Test
    fun tapUnfocusedText_onlyMovesFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(viewModel.text(MY_SECOND_TEXT_ID)))

            expectNoEvents()
        }
        assertEquals(CanvasToppingKey.Text(MY_SECOND_TEXT_ID), viewModel.state.value.focusedKey)
        assertNull(textEditSessionHolder.session())
    }

    @Test
    fun cleared_dropsTheResultItLeftUnconsumed() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        val store = ViewModelStore().apply { put("arrange", viewModel) }
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(viewModel.text(SHARED_ID)))
        // 편집하는 사이 텍스트가 서버에서 사라져 결과를 받을 곳이 없다
        todayCanvases.value = textCanvas().copy(
            texts = textCanvas().texts.filterNot { it.parfaitTextId == ParfaitTextId(SHARED_ID) },
        )
        advanceUntilIdle()
        textEditSessionHolder.finish(edited(text = "새 글"))
        advanceUntilIdle()
        assertEquals(SHARED_ID, textEditSessionHolder.result.value?.first)

        store.clear()

        assertNull(textEditSessionHolder.result.value)
    }

    @Test
    fun cleared_leavesAnEditItDidNotStart() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        val store = ViewModelStore().apply { put("arrange", viewModel) }
        val othersSession = TextEditSession(
            groupId = GROUP_ID,
            parfaitId = PARFAIT_ID,
            original = viewModel.text(SHARED_ID),
        )
        textEditSessionHolder.start(othersSession)

        store.clear()

        assertEquals(othersSession, textEditSessionHolder.session())
    }

    @Test
    fun editedWithNewContent_updatesLocallyAndMarksForRecreate() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        returnFromTextEdit(SHARED_ID, edited(text = "새 글"), viewModel)

        assertEquals("새 글", viewModel.text(SHARED_ID).text)
        assertEquals(setOf(SHARED_ID), viewModel.state.value.recreateTextIds)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
        assertNull(textEditSessionHolder.result.value)

        // 폴링이 서버 값을 다시 줘도 고친 글을 덮지 않는다
        todayCanvases.value = textCanvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()
        assertEquals("새 글", viewModel.text(SHARED_ID).text)
    }

    @Test
    fun editedWithSameContentButNewColor_isDirtyNotRecreate() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        returnFromTextEdit(SHARED_ID, edited(color = TextToppingColor.Pink), viewModel)

        assertEquals(TextToppingColor.Pink, viewModel.text(SHARED_ID).color)
        assertTrue(
            viewModel.state.value.recreateTextIds
                .isEmpty(),
        )
        assertTrue(SHARED_ID in viewModel.state.value.dirtyTextIds)
    }

    @Test
    fun unchangedEditedResult_isNotDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        // 위치는 float 왕복 오차만큼만 다르다
        returnFromTextEdit(SHARED_ID, edited().copy(positionX = 0.40001f), viewModel)

        assertTrue(
            viewModel.state.value.dirtyTextIds
                .isEmpty(),
        )
        assertFalse(viewModel.state.value.hasUnsavedChanges)
        assertNull(textEditSessionHolder.result.value)
    }

    @Test
    fun editedResultWithNewColor_isDirty() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        returnFromTextEdit(SHARED_ID, edited(color = TextToppingColor.Pink), viewModel)

        assertEquals(setOf(SHARED_ID), viewModel.state.value.dirtyTextIds)
        assertTrue(viewModel.state.value.hasUnsavedChanges)
    }

    @Test
    fun deletedResult_marksTextForDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()

        returnFromTextEdit(SHARED_ID, TextEditResult.Deleted, viewModel)

        assertEquals(setOf(SHARED_ID), viewModel.state.value.pendingDeleteTextIds)
        assertTrue(
            viewModel.state.value.texts
                .none { it.parfaitTextId == SHARED_ID },
        )
        assertNull(viewModel.state.value.focusedKey)
    }

    private fun placedText(id: Long) = PlacedTextToppingVO(
        parfaitTextId = ParfaitTextId(id),
        text = "새 글",
        transform = ToppingTransform(positionX = 0.4, positionY = 0.6, positionZ = 2, scale = 1.2, rotation = 30.0),
        style = TextToppingStyle(color = "#000000", fontSize = 18.0, isFill = false, isStroke = false),
        placedBy = ToppingPlacerVO(groupMemberId = GroupMemberId(MY_MEMBER_ID), nickname = GroupNickname("나")),
    )

    @Test
    fun confirm_savesBatchThenStylesThenRecreatesThenDeletes() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        returnFromTextEdit(SHARED_ID, edited(text = "새 글"), viewModel)
        returnFromTextEdit(MY_SECOND_TEXT_ID, edited(color = TextToppingColor.Pink), viewModel)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickTopping(viewModel.topping(SHARED_ID)))
        viewModel.drag()
        val textUpdates = slot<List<TextToppingTransformUpdate>>()
        coEvery { updateToppings(any(), any(), any(), capture(textUpdates)) } returns Result.success(emptyList())
        val styleUpdate = slot<TextToppingUpdate>()
        coEvery { updateTextTopping(any(), any(), capture(styleUpdate)) } returns Result.success(
            mockk<UpdatedTextToppingVO>(),
        )
        val placedTransform = slot<ToppingTransform>()
        coEvery {
            placeTextTopping(any(), any(), any(), capture(placedTransform), any())
        } returns Result.success(placedText(99L))
        coEvery { deleteTextTopping(any(), any(), any()) } returns Result.success(Unit)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerifyOrder {
            updateToppings(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), any(), any())
            updateTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), any())
            placeTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), "새 글", any(), any())
            deleteTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitTextId(SHARED_ID))
            refreshTodayParfaitDetail(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID))
        }
        // 다시 만들 텍스트는 일괄 PATCH 에 싣지 않는다
        assertTrue(textUpdates.captured.none { it.parfaitTextId == ParfaitTextId(SHARED_ID) })
        // 스타일 PATCH 는 스타일 축만 싣는다
        assertEquals(
            TextToppingUpdate(
                parfaitTextId = ParfaitTextId(MY_SECOND_TEXT_ID),
                color = TextToppingColor.Pink.toWireColor(),
                fontSize = 18.0,
                isFill = false,
                isStroke = false,
            ),
            styleUpdate.captured,
        )
        // 새 텍스트는 원래 겹침 순서를 이어받는다
        assertEquals(2, placedTransform.captured.positionZ)
        assertEquals(0.4, placedTransform.captured.positionX, 1e-6)
        assertEquals(1.2, placedTransform.captured.scale, 1e-6)
        coVerify(exactly = 1) { updateTextTopping(any(), any(), any()) }
    }

    @Test
    fun confirm_recreateDeleteFails_retriesOnlyTheDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = textViewModel()
        returnFromTextEdit(SHARED_ID, edited(text = "새 글"), viewModel)
        coEvery {
            placeTextTopping(any(), any(), any(), any(), any())
        } returns Result.success(placedText(99L))
        coEvery { deleteTextTopping(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_SAVE_UNKNOWN),
                awaitItem(),
            )
        }
        assertTrue(
            viewModel.state.value.recreateTextIds
                .isEmpty(),
        )
        assertEquals(setOf(SHARED_ID), viewModel.state.value.pendingDeleteTextIds)
        assertFalse(viewModel.state.value.isLoading)

        coEvery { deleteTextTopping(any(), any(), any()) } returns Result.success(Unit)
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { placeTextTopping(any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) {
            deleteTextTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitTextId(SHARED_ID))
        }
    }

    private fun textVO(
        parfaitTextId: Long,
        isMine: Boolean = true,
        positionZ: Int,
    ) = CanvasTextToppingVO(
        parfaitTextId = ParfaitTextId(parfaitTextId),
        text = "안녕",
        transform = ToppingTransform(
            positionX = 0.4,
            positionY = 0.6,
            positionZ = positionZ,
            scale = 1.2,
            rotation = 30.0,
        ),
        style = TextToppingStyle(color = "#000000", fontSize = 18.0, isFill = false, isStroke = false),
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(if (isMine) MY_MEMBER_ID else OTHER_MEMBER_ID),
            nickname = GroupNickname("올린이"),
        ),
        isMine = isMine,
        createdAt = LocalDateTime(2026, 10, 26, 9, 0),
    )

    private fun canvas(
        parfaitId: Long = PARFAIT_ID,
        toppings: List<CanvasToppingVO> = listOf(
            toppingVO(FIRST_ID, positionZ = 1),
            toppingVO(SECOND_ID, positionZ = 2),
            toppingVO(OTHERS_ID, isMine = false, positionZ = 3),
        ),
        texts: List<CanvasTextToppingVO> = emptyList(),
    ) = CanvasVO(
        parfaitId = ParfaitId(parfaitId),
        groupName = GroupName("아메리카노"),
        date = parfaitToday(),
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = toppings,
        texts = texts,
    )

    private fun toppingVO(
        parfaitImageId: Long,
        isMine: Boolean = true,
        positionZ: Int = parfaitImageId.toInt(),
        positionX: Double = 0.25,
        border: ToppingBorder = ToppingBorder.None,
    ) = CanvasToppingVO(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        imageId = ImageId(parfaitImageId),
        imageUrl = "https://cdn.example.com/topping-$parfaitImageId.png",
        transform = ToppingTransform(
            positionX = positionX,
            positionY = 0.75,
            positionZ = positionZ,
            scale = 1.0,
            rotation = 0.0,
        ),
        border = border,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(if (isMine) MY_MEMBER_ID else OTHER_MEMBER_ID),
            nickname = GroupNickname("올린이"),
        ),
        isMine = isMine,
        createdAt = LocalDateTime(2026, 8, 19, 9, 0),
    )
}
