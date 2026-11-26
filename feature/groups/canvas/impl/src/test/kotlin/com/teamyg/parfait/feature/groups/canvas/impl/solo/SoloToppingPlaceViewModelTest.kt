package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.PlaceSoloToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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

private const val SUBJECT_PATH = "/cache/segmentation/subject.png"

private val CANVAS_SIZE = DpSize(360.dp, 640.dp)

private val TOPPING_BASE_SIZE = DpSize(100.dp, 50.dp)

class SoloToppingPlaceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getToppingDraftFlow: GetToppingDraftFlowUseCase = mockk()
    private val clearToppingDraft: ClearToppingDraftUseCase = mockk(relaxed = true)
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase = mockk()
    private val placeSoloTopping: PlaceSoloToppingUseCase = mockk()
    private val addRecentImage: AddRecentImageUseCase = mockk(relaxed = true)

    @Before
    fun stubTheHappyPath() {
        every { getSoloCanvasFlow() } returns flowOf(
            Result.success(SoloCanvasVO(background = null, toppings = emptyList(), texts = emptyList())),
        )
        coEvery { placeSoloTopping(any(), any(), any()) } returns Result.success(Unit)
    }

    private fun soloDraft() = ToppingDraft(
        target = ToppingDraftTarget.Solo,
        nextPositionZ = 3,
        subjectImagePath = SUBJECT_PATH,
        cutoutImagePath = "/cache/segmentation/cutout.png",
    )

    private fun groupDraft() = soloDraft().copy(target = ToppingDraftTarget.Group(GroupId(1L), ParfaitId(2L)))

    private fun TestScope.shownViewModel(draft: ToppingDraft?): SoloToppingPlaceViewModel {
        every { getToppingDraftFlow() } returns flowOf(draft)
        return SoloToppingPlaceViewModel(
            getToppingDraftFlow = getToppingDraftFlow,
            clearToppingDraft = clearToppingDraft,
            getSoloCanvasFlow = getSoloCanvasFlow,
            placeSoloTopping = placeSoloTopping,
            addRecentImage = addRecentImage,
        ).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()
        }
    }

    private fun SoloToppingPlaceViewModel.measureAndReady() {
        processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(CANVAS_SIZE))
        processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(TOPPING_BASE_SIZE))
        processIntent(CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady = true))
    }

    @Test
    fun soloDraft_confirm_placesOnceWithInitialPlacement_thenClearsAndReturns() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 혼자 공간 초안, 빈 캔버스, 실측을 마친 상태
            val viewModel = shownViewModel(soloDraft())
            viewModel.measureAndReady()

            viewModel.effect.test {
                // When 손대지 않고 확인을 누른다
                viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
                advanceUntilIdle()

                // Then 캔버스로 돌아간다
                assertEquals(SoloToppingPlaceEffect.PlaceSucceeded, awaitItem())
            }

            // 정중앙·긴 변 40% 초기 배치, z 는 빈 캔버스의 맨 위(1)다
            val expectedTransform = toToppingTransform(
                offsetX = 130.dp,
                offsetY = 295.dp,
                scale = 1.44f,
                rotationDegrees = 0f,
                canvasSize = CANVAS_SIZE,
                toppingBaseSize = TOPPING_BASE_SIZE,
                positionZ = 1,
            )
            coVerify(exactly = 1) {
                placeSoloTopping(filePath = SUBJECT_PATH, transform = expectedTransform, border = ToppingBorder.None)
            }
            coVerify(exactly = 1) { addRecentImage(source = SUBJECT_PATH, kind = RecentImageKind.CUTOUT) }
            coVerify(exactly = 1) { clearToppingDraft() }
        }

    @Test
    fun groupDraft_quitsToCanvasWithoutPlacing() = runTest(mainDispatcherRule.dispatcher) {
        every { getToppingDraftFlow() } returns flowOf(groupDraft())

        val viewModel = SoloToppingPlaceViewModel(
            getToppingDraftFlow = getToppingDraftFlow,
            clearToppingDraft = clearToppingDraft,
            getSoloCanvasFlow = getSoloCanvasFlow,
            placeSoloTopping = placeSoloTopping,
            addRecentImage = addRecentImage,
        )

        viewModel.effect.test {
            advanceUntilIdle()
            assertEquals(SoloToppingPlaceEffect.QuitToCanvas, awaitItem())

            // 확인을 눌러도 놓을 것이 없다
            viewModel.measureAndReady()
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()
            assertEquals(SoloToppingPlaceEffect.DraftMissing, awaitItem())
        }

        coVerify(exactly = 0) { placeSoloTopping(any(), any(), any()) }
        coVerify(exactly = 0) { clearToppingDraft() }
    }

    @Test
    fun placeFailure_emitsLocalFailureAndKeepsDraft() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { placeSoloTopping(any(), any(), any()) } returns Result.failure(IOException("disk full"))
        val viewModel = shownViewModel(soloDraft())
        viewModel.measureAndReady()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingPlaceEffect.PlaceFailed, awaitItem())
        }

        assertEquals(false, viewModel.state.value.isLoading)
        coVerify(exactly = 0) { clearToppingDraft() }
        coVerify(exactly = 0) { addRecentImage(any(), any()) }
    }

    @Test
    fun confirmBeforeImageReady_doesNotPlace() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel(soloDraft())
        viewModel.processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(CANVAS_SIZE))
        viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(TOPPING_BASE_SIZE))

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(SoloToppingPlaceEffect.ToppingImageNotReady, awaitItem())
        }

        coVerify(exactly = 0) { placeSoloTopping(any(), any(), any()) }
    }
}
