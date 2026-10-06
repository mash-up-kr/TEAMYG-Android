package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
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
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BORDER_WIDTH_RANGE_DP
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_MIN_SCALE
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
    private val updateToppings: UpdateToppingsUseCase = mockk()
    private val updateToppingBorder: UpdateToppingBorderUseCase = mockk()

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
    ): CanvasToppingArrangeViewModel {
        stubTheHappyPath(initialCanvas)
        return CanvasToppingArrangeViewModel(
            groupIdValue = GROUP_ID,
            parfaitIdValue = PARFAIT_ID,
            initialToppingId = initialToppingId,
            getTodayParfaitFlowUseCase = getTodayParfaitFlow,
            refreshTodayParfaitDetailUseCase = refreshTodayParfaitDetail,
            deleteToppingUseCase = deleteTopping,
            updateToppingsUseCase = updateToppings,
            updateToppingBorderUseCase = updateToppingBorder,
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

        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
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

        assertEquals(SECOND_ID, viewModel.state.value.focusedToppingId)
        assertFalse(viewModel.state.value.isBorderPanelOpen)
    }

    @Test
    fun clickOthersTopping_showsToastAndKeepsFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.click(OTHERS_ID)

            assertEquals(CanvasToppingArrangeEffect.ShowOthersToppingNotEditable, awaitItem())
        }
        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
    }

    @Test
    fun clickEmptyCanvas_clearsFocus() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)

        assertNull(viewModel.state.value.focusedToppingId)
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
        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
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
        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
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
        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
    }

    @Test
    fun clickEmptyCanvas_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
    }

    @Test
    fun clickDeleteButton_whilePanelOpen_onlyClosesPanel() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertFalse(viewModel.state.value.showDeleteToppingDialog)
    }

    @Test
    fun focusedToppingRemovedByPolling_closesPanel_andSendsNoPatch() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.click(FIRST_ID)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(BLACK_ARGB))
        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID), toppingVO(OTHERS_ID, isMine = false)))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isBorderPanelOpen)
        assertNull(viewModel.state.value.focusedToppingId)
        assertFalse(FIRST_ID in viewModel.state.value.dirtyToppingIds)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 0) { updateToppingBorder(any(), any(), ParfaitImageId(FIRST_ID), any()) }
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
    }

    @Test
    fun focusedToppingRemovedByPolling_closesDeleteDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        assertTrue(viewModel.state.value.showDeleteToppingDialog)

        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID), toppingVO(OTHERS_ID, isMine = false)))
        advanceUntilIdle()

        assertNull(viewModel.state.value.focusedToppingId)
        assertFalse(viewModel.state.value.showDeleteToppingDialog)
    }

    @Test
    fun otherToppingRemovedByPolling_keepsDeleteDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        todayCanvases.value = canvas(toppings = listOf(toppingVO(FIRST_ID), toppingVO(OTHERS_ID, isMine = false)))
        advanceUntilIdle()

        assertEquals(FIRST_ID, viewModel.state.value.focusedToppingId)
        assertTrue(viewModel.state.value.showDeleteToppingDialog)
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
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        val refreshGate = holdTheRefresh()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack)

            // 여기서 되감으면 삭제가 낼 되감기와 겹친다
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
    fun deleteConfirm_success_refreshesThenNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery {
            deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(FIRST_ID))
        } returns Result.success(Unit)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        assertTrue(viewModel.state.value.showDeleteToppingDialog)
        val refreshGate = holdTheRefresh()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)
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
        assertFalse(viewModel.state.value.showDeleteToppingDialog)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun deleteConfirm_refreshThrows_stillNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        coEvery { refreshTodayParfaitDetail(any(), any()) } throws RuntimeException("갱신 실패")

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)

            // 토핑은 이미 지워졌다. 갱신이 터졌다고 삭제 실패로 알리지 않는다
            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun deleteConfirm_success_discardsOtherDirtyToppings() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        viewModel.click(SECOND_ID)
        viewModel.drag()
        viewModel.click(FIRST_ID)
        assertTrue(SECOND_ID in viewModel.state.value.dirtyToppingIds)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)

            assertEquals(CanvasToppingArrangeEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { updateToppings(any(), any(), any()) }
        coVerify(exactly = 0) { updateToppingBorder(any(), any(), any(), any()) }
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

    @Test
    fun deleteConfirm_failure_showsErrorAndStays() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)

            assertEquals(
                CanvasToppingArrangeEffect.ShowError(CanvasToppingArrangeError.TOPPING_DELETE_UNKNOWN),
                awaitItem(),
            )
            expectNoEvents()
        }
        assertTrue(
            viewModel.state.value.toppings
                .any { it.parfaitImageId == FIRST_ID },
        )
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
        // 삭제가 실패하는 경로로는 툼스톤이 서지 않으므로, 성공하되 화면이 남아 있는 상태를 만든다
        val viewModel = viewModel()
        val deleteGate = CompletableDeferred<Unit>()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        coEvery { refreshTodayParfaitDetail(any(), any()) } coAnswers {
            deleteGate.await()
            Result.success(Unit)
        }
        viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()
        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == FIRST_ID },
        )

        // 삭제 직전에 출발한 응답이 뒤늦게 도착한다
        todayCanvases.value = canvas().copy(lastClosedDate = parfaitToday())
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == FIRST_ID },
        )
        deleteGate.complete(Unit)
    }

    @Test
    fun merge_whenTheServerDropsIt_clearsTheTombstone() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        val deleteGate = CompletableDeferred<Unit>()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.success(Unit)
        coEvery { refreshTodayParfaitDetail(any(), any()) } coAnswers {
            deleteGate.await()
            Result.success(Unit)
        }
        viewModel.processIntent(CanvasToppingArrangeIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()
        assertEquals(setOf(FIRST_ID), viewModel.state.value.deletedToppingIds)

        // 서버가 그 토핑을 뺀 응답을 주면 툼스톤이 빈다
        todayCanvases.value = canvas(toppings = listOf(toppingVO(SECOND_ID)))
        advanceUntilIdle()

        assertTrue(
            viewModel.state.value.deletedToppingIds
                .isEmpty(),
        )
        deleteGate.complete(Unit)
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

    private fun canvas(
        parfaitId: Long = PARFAIT_ID,
        toppings: List<CanvasToppingVO> = listOf(
            toppingVO(FIRST_ID, positionZ = 1),
            toppingVO(SECOND_ID, positionZ = 2),
            toppingVO(OTHERS_ID, isMine = false, positionZ = 3),
        ),
    ) = CanvasVO(
        parfaitId = ParfaitId(parfaitId),
        groupName = GroupName("아메리카노"),
        date = parfaitToday(),
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = null,
        toppings = toppings,
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
