package com.teamyg.parfait.feature.groups.canvas.impl.solo

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.TextToppingAlign
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloTextUseCase
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloToppingUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.UpdateSoloToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.solo.UpdateSoloTransformsUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasToppingKey
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val FIRST_ID = 1L
private const val SECOND_ID = 2L
private const val TEXT_ID = 3L

private val ORANGE_ARGB = 0xFFFF6B00.toInt()

class SoloToppingArrangeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase = mockk()
    private val updateSoloTransforms: UpdateSoloTransformsUseCase = mockk()
    private val updateSoloToppingBorder: UpdateSoloToppingBorderUseCase = mockk()
    private val deleteSoloTopping: DeleteSoloToppingUseCase = mockk()
    private val deleteSoloText: DeleteSoloTextUseCase = mockk()

    private val soloCanvas = MutableStateFlow(Result.success(canvas()))

    @Before
    fun stubTheHappyPath() {
        every { getSoloCanvasFlow() } returns soloCanvas
        coEvery { updateSoloTransforms(any(), any()) } returns Result.success(Unit)
        coEvery { updateSoloToppingBorder(any(), any()) } returns Result.success(Unit)
        coEvery { deleteSoloTopping(any()) } returns Result.success(Unit)
        coEvery { deleteSoloText(any()) } returns Result.success(Unit)
    }

    private fun TestScope.shownViewModel(
        initialToppingId: Long = FIRST_ID,
        initialIsText: Boolean = false,
    ): SoloToppingArrangeViewModel = SoloToppingArrangeViewModel(
        initialToppingId = initialToppingId,
        initialIsText = initialIsText,
        getSoloCanvasFlow = getSoloCanvasFlow,
        updateSoloTransforms = updateSoloTransforms,
        updateSoloToppingBorder = updateSoloToppingBorder,
        deleteSoloTopping = deleteSoloTopping,
        deleteSoloText = deleteSoloText,
    ).also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    private fun SoloToppingArrangeViewModel.currentState() = state.value

    private fun SoloToppingArrangeViewModel.toppingIds() = currentState().toppings.map { it.parfaitImageId }

    /** 음수 임계값이면 중앙 가이드 스냅이 걸리지 않는다 */
    private fun SoloToppingArrangeViewModel.drag(panX: Float = 0.1f) = processIntent(
        CanvasToppingArrangeIntent.OnToppingTransform(
            panX = panX,
            panY = 0f,
            zoom = 1f,
            rotationDelta = 0f,
            thresholdRatioX = -1f,
            thresholdRatioY = -1f,
        ),
    )

    @Test
    fun firstEmission_focusesInitialText() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = TEXT_ID, initialIsText = true)

        assertEquals(CanvasToppingKey.Text(TEXT_ID), viewModel.state.value.focusedKey)
        assertEquals(listOf(FIRST_ID, SECOND_ID), viewModel.toppingIds())
    }

    @Test
    fun movedToppingAndText_confirm_savesOnlyChangedOnesOnce_thenNavigatesBack() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = shownViewModel(initialToppingId = FIRST_ID)
            val updates = slot<List<ToppingTransformUpdate>>()
            val textUpdates = slot<List<TextToppingTransformUpdate>>()
            coEvery { updateSoloTransforms(capture(updates), capture(textUpdates)) } returns Result.success(Unit)

            // Given 첫 토핑과 텍스트를 옮긴다. 둘째 토핑은 손대지 않는다
            viewModel.drag(panX = 0.1f)
            viewModel.processIntent(
                CanvasToppingArrangeIntent.OnClickText(viewModel.currentState().texts.first()),
            )
            viewModel.drag(panX = 0.2f)

            viewModel.effect.test {
                // When 확인을 누른다
                viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
                advanceUntilIdle()

                // Then 바뀐 것만 한 번에 저장하고 돌아간다
                assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
            }

            coVerify(exactly = 1) { updateSoloTransforms(any(), any()) }
            assertEquals(listOf(ParfaitImageId(FIRST_ID)), updates.captured.map { it.parfaitImageId })
            assertEquals(0.35, updates.captured.single().positionX!!, absoluteTolerance = 1e-6)
            val textUpdate = textUpdates.captured.single()
            assertEquals(ParfaitTextId(TEXT_ID), textUpdate.parfaitTextId)
            assertEquals(0.6, textUpdate.positionX!!, absoluteTolerance = 1e-6)
            // 손대지 않은 축은 싣지 않는다
            assertEquals(null, textUpdate.positionY)
            coVerify(exactly = 0) { updateSoloToppingBorder(any(), any()) }
            coVerify(exactly = 0) { deleteSoloTopping(any()) }
            coVerify(exactly = 0) { deleteSoloText(any()) }
        }

    @Test
    fun noChanges_confirm_savesNothing_andNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 0) { updateSoloTransforms(any(), any()) }
        coVerify(exactly = 0) { updateSoloToppingBorder(any(), any()) }
        coVerify(exactly = 0) { deleteSoloTopping(any()) }
        coVerify(exactly = 0) { deleteSoloText(any()) }
    }

    @Test
    fun borderColorSelected_confirm_savesBorderOnlyForThatTopping() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = SECOND_ID)
        val border = slot<ToppingBorder>()
        coEvery { updateSoloToppingBorder(ParfaitImageId(SECOND_ID), capture(border)) } returns Result.success(Unit)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel)
        viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(ORANGE_ARGB))
        viewModel.processIntent(CanvasToppingArrangeIntent.OnDismissBorderPanel)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { updateSoloToppingBorder(any(), any()) }
        assertTrue(border.captured is ToppingBorder.Solid)
        // 테두리만 바꿨으니 변형은 보내지 않는다
        coVerify(exactly = 0) { updateSoloTransforms(any(), any()) }
    }

    @Test
    fun deletedTopping_confirm_deletesThatTopping() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = SECOND_ID)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
        assertEquals(listOf(FIRST_ID), viewModel.toppingIds())

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { deleteSoloTopping(ParfaitImageId(SECOND_ID)) }
        coVerify(exactly = 0) { deleteSoloText(any()) }
        coVerify(exactly = 0) { updateSoloTransforms(any(), any()) }
    }

    @Test
    fun deletedText_confirm_deletesThatText() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = TEXT_ID, initialIsText = true)

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) { deleteSoloText(ParfaitTextId(TEXT_ID)) }
        coVerify(exactly = 0) { deleteSoloTopping(any()) }
    }

    @Test
    fun saveFails_showsSaveFailed_keepsScreen_andSkipsDeletes() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = FIRST_ID)
        coEvery { updateSoloTransforms(any(), any()) } returns Result.failure(IOException("disk full"))

        viewModel.drag()
        viewModel.processIntent(
            CanvasToppingArrangeIntent.OnClickTopping(viewModel.currentState().toppings.last()),
        )
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            // 기기 안 저장이라 네트워크 문구가 아니라 저장 실패 문구다
            assertEquals(SoloToppingArrangeEffect.SaveFailed, awaitItem())
            expectNoEvents()
        }

        // 삭제는 되돌릴 수 없어 저장이 실패하면 보내지 않는다
        coVerify(exactly = 0) { deleteSoloTopping(any()) }
        assertFalse(viewModel.state.value.isLoading)
        // 다시 누른 확인이 같은 토핑을 다시 보낸다
        assertTrue(viewModel.state.value.hasUnsavedChanges)
    }

    @Test
    fun deleteFails_showsDeleteFailed_andKeepsPendingDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(initialToppingId = SECOND_ID)
        coEvery { deleteSoloTopping(any()) } returns Result.failure(IOException("disk full"))

        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingArrangeEffect.DeleteFailed, awaitItem())
            expectNoEvents()
        }

        assertEquals(setOf(SECOND_ID), viewModel.state.value.pendingDeleteToppingIds)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun close_withUnsavedChanges_asksBeforeLeaving() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel()

        viewModel.drag()
        viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose)

        assertTrue(viewModel.state.value.showQuitDialog)
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogConfirm)
            assertEquals(SoloToppingArrangeEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { updateSoloTransforms(any(), any()) }
    }

    private fun canvas() = SoloCanvasVO(
        background = null,
        toppings = listOf(toppingVO(FIRST_ID, positionZ = 1), toppingVO(SECOND_ID, positionZ = 2)),
        texts = listOf(textVO(TEXT_ID)),
    )

    private fun toppingVO(
        id: Long,
        positionZ: Int,
    ) = CanvasToppingVO(
        parfaitImageId = ParfaitImageId(id),
        imageId = ImageId(id),
        imageUrl = "/files/solo/topping-$id.png",
        transform = ToppingTransform(
            positionX = 0.25,
            positionY = 0.75,
            positionZ = positionZ,
            scale = 1.0,
            rotation = 0.0,
        ),
        border = ToppingBorder.None,
        placedBy = placer(),
        isMine = true,
        createdAt = LocalDateTime(2026, 11, 26, 9, 0),
    )

    private fun textVO(id: Long) = CanvasTextToppingVO(
        parfaitTextId = ParfaitTextId(id),
        text = "안녕",
        transform = ToppingTransform(positionX = 0.4, positionY = 0.6, positionZ = 3, scale = 1.2, rotation = 30.0),
        style = TextToppingStyle(
            color = "#000000",
            fontSize = 18.0,
            isFill = false,
            isStroke = false,
            textAlign = TextToppingAlign.CENTER,
        ),
        placedBy = placer(),
        isMine = true,
        createdAt = LocalDateTime(2026, 11, 26, 9, 0),
    )

    private fun placer() = ToppingPlacerVO(groupMemberId = GroupMemberId(0), nickname = GroupNickname("나"))
}
