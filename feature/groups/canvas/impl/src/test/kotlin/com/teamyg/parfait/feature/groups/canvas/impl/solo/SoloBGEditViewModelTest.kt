package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.extension.toRgbHex
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.usecase.solo.ChangeSoloBackgroundUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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
import kotlin.test.assertNull

private const val LOCAL_IMAGE_URI = "content://media/external/images/media/42"
private const val STORED_IMAGE_PATH = "/data/user/0/app/cache/solo/background.jpg"

class SoloBGEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase = mockk()
    private val changeSoloBackground: ChangeSoloBackgroundUseCase = mockk()

    private val soloCanvases = MutableStateFlow<Result<SoloCanvasVO>?>(Result.success(canvas()))

    @Before
    fun stubTheHappyPath() {
        every { getSoloCanvasFlow() } returns soloCanvases.filterNotNull()
        coEvery { changeSoloBackground.color(any()) } returns Result.success(Unit)
        coEvery { changeSoloBackground.image(any()) } returns Result.success(Unit)
    }

    private fun TestScope.shownViewModel() = SoloBGEditViewModel(
        getSoloCanvasFlow = getSoloCanvasFlow,
        changeSoloBackground = changeSoloBackground,
    ).also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    @Test
    fun init_seedsSavedColorBackground() = runTest(mainDispatcherRule.dispatcher) {
        soloCanvases.value = Result.success(canvas(CanvasBackground.Color("#FF6B00")))

        val viewModel = shownViewModel()

        assertEquals(Color(0xFFFF6B00), viewModel.state.value.selectedColor)
        assertNull(viewModel.state.value.selectedImageUri)
    }

    @Test
    fun init_seedsSavedImageBackgroundAsAlreadyStored() = runTest(mainDispatcherRule.dispatcher) {
        soloCanvases.value = Result.success(canvas(CanvasBackground.Image(STORED_IMAGE_PATH)))

        val viewModel = shownViewModel()

        assertEquals(STORED_IMAGE_PATH, viewModel.state.value.selectedImageUri)
        assertNull(viewModel.state.value.selectedImageSource)
    }

    @Test
    fun confirmAfterSelectingColor_savesColorOnce_thenCloses() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel()
        val picked = Color(0xFFC2E4FC)

        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnSelectColor(picked))
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            assertEquals(SoloBGEditEffect.Close, awaitItem())
        }
        coVerify(exactly = 1) { changeSoloBackground.color(picked.toRgbHex()) }
        coVerify(exactly = 0) { changeSoloBackground.image(any()) }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun confirmAfterPickingPhoto_savesImageOnce_thenCloses() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel()

        viewModel.effect.test {
            viewModel.processIntent(
                CanvasBGEditIntent.OnBackgroundImageResult(LOCAL_IMAGE_URI, PictureConfirmSource.GALLERY),
            )
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            assertEquals(SoloBGEditEffect.Close, awaitItem())
        }
        coVerify(exactly = 1) { changeSoloBackground.image(LOCAL_IMAGE_URI) }
        coVerify(exactly = 0) { changeSoloBackground.color(any()) }
    }

    @Test
    fun confirmWithStoredImageUntouched_savesNothing_butCloses() = runTest(mainDispatcherRule.dispatcher) {
        soloCanvases.value = Result.success(canvas(CanvasBackground.Image(STORED_IMAGE_PATH)))
        val viewModel = shownViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            assertEquals(SoloBGEditEffect.Close, awaitItem())
        }
        coVerify(exactly = 0) { changeSoloBackground.image(any()) }
        coVerify(exactly = 0) { changeSoloBackground.color(any()) }
    }

    @Test
    fun saveFailure_showsNonNetworkError_andStays() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { changeSoloBackground.image(any()) } returns Result.failure(IOException("disk full"))
        val viewModel = shownViewModel()

        viewModel.effect.test {
            viewModel.processIntent(
                CanvasBGEditIntent.OnBackgroundImageResult(LOCAL_IMAGE_URI, PictureConfirmSource.CAMERA),
            )
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            assertEquals(SoloBGEditEffect.ShowSaveFailed, awaitItem())
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun quitWithoutChanges_closesWithoutCallingUseCases() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = shownViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickCloseButton)
            viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogConfirm)

            assertEquals(SoloBGEditEffect.NavigateBack, awaitItem())
        }
        coVerify(exactly = 0) { changeSoloBackground.color(any()) }
        coVerify(exactly = 0) { changeSoloBackground.image(any()) }
    }

    private fun canvas(background: CanvasBackground? = null) =
        SoloCanvasVO(background = background, toppings = emptyList(), texts = emptyList())
}
