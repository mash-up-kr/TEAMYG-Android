package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.PlaceSoloTextUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.textToppingStyleOf
import com.teamyg.parfait.feature.groups.canvas.impl.util.toTextToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val CANVAS_SIZE = DpSize(200.dp, 400.dp)

private val BOX_SIZE = DpSize(100.dp, 40.dp)

class SoloTextInputViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()
    private val clearToppingDraft: ClearToppingDraftUseCase = mockk(relaxed = true)
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase = mockk()
    private val placeSoloText: PlaceSoloTextUseCase = mockk()

    private fun soloDraft() = ToppingDraft(target = ToppingDraftTarget.Solo, nextPositionZ = 3)

    private fun TestScope.shownViewModel(draft: ToppingDraft?): SoloTextInputViewModel {
        every { getToppingDraftFlow() } returns flowOf(draft)
        every { getSoloCanvasFlow() } returns flowOf(
            Result.success(SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList())),
        )
        coEvery { placeSoloText(any(), any(), any()) } returns Result.success(Unit)
        return SoloTextInputViewModel(
            getToppingDraftFlow = getToppingDraftFlow,
            clearToppingDraft = clearToppingDraft,
            getSoloCanvasFlow = getSoloCanvasFlow,
            placeSoloText = placeSoloText,
        ).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()
        }
    }

    private fun SoloTextInputViewModel.typeAndMeasure(content: String) {
        processIntent(CanvasTextInputIntent.OnContentChange(content))
        processIntent(CanvasTextInputIntent.OnCanvasMeasured(CANVAS_SIZE))
        processIntent(CanvasTextInputIntent.OnTextBoxMeasured(BOX_SIZE))
    }

    @Test
    fun confirmWithContent_placesOnceWithDraftZ_thenClearsAndReturns() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())
        viewModel.typeAndMeasure("hi")
        val state = viewModel.state.value

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(SoloTextInputEffect.NavigateBack, awaitItem())
        }

        coVerify(exactly = 1) {
            placeSoloText(
                text = "hi",
                transform = toTextToppingTransform(
                    offsetX = state.offsetX,
                    offsetY = state.offsetY,
                    boxSize = BOX_SIZE,
                    canvasSize = CANVAS_SIZE,
                    scale = state.scale,
                    rotationDegrees = state.rotationDegrees,
                    positionZ = 3,
                ),
                style = textToppingStyleOf(state.selectedColor, state.textType, state.align, state.designFontSize),
            )
        }
        coVerify(exactly = 1) { clearToppingDraft() }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun blankContent_stackDoesNotPlace() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())
        viewModel.typeAndMeasure("  \n ")

        viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
        advanceUntilIdle()

        coVerify(exactly = 0) { placeSoloText(any(), any(), any()) }
    }

    @Test
    fun blankContent_confirmClearsDraftAndReturns() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloTextInputEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 1) { clearToppingDraft() }
        coVerify(exactly = 0) { placeSoloText(any(), any(), any()) }
    }

    @Test
    fun groupDraft_returnsWithoutPlacing() = runTest(mainDispatcherRule.dispatcher) {
        every { getToppingDraftFlow() } returns flowOf(
            soloDraft().copy(target = ToppingDraftTarget.Group(GroupId(1L), ParfaitId(2L))),
        )
        every { getSoloCanvasFlow() } returns flowOf(
            Result.success(SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList())),
        )
        val viewModel = SoloTextInputViewModel(getToppingDraftFlow, clearToppingDraft, getSoloCanvasFlow, placeSoloText)

        viewModel.effect.test {
            advanceUntilIdle()
            assertEquals(SoloTextInputEffect.NavigateBack, awaitItem())

            viewModel.typeAndMeasure("hi")
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify(exactly = 0) { placeSoloText(any(), any(), any()) }
    }

    @Test
    fun placeFailure_keepsDraftAndContent_andShowsNonNetworkFailure() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())
        coEvery { placeSoloText(any(), any(), any()) } returns Result.failure(IOException("disk"))
        viewModel.typeAndMeasure("hi")

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickStack)
            advanceUntilIdle()

            assertEquals(SoloTextInputEffect.PlaceFailed, awaitItem())
        }
        coVerify(exactly = 0) { clearToppingDraft() }
        assertEquals("hi", viewModel.state.value.content)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun quitDialogConfirm_clearsDraftAndReturns() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())

        viewModel.effect.test {
            viewModel.processIntent(CanvasTextInputIntent.OnClickClose)
            viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogConfirm)
            advanceUntilIdle()

            assertEquals(SoloTextInputEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 1) { clearToppingDraft() }
    }
}
