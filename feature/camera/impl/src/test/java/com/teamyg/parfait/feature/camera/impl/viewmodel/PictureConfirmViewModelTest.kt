package com.teamyg.parfait.feature.camera.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.analytics.AnalyticsEvent
import com.teamyg.parfait.core.analytics.AnalyticsEventType
import com.teamyg.parfait.core.analytics.AnalyticsLogger
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.image.PrepareSegmentationModuleUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

private const val URI = "content://media/external/images/1"

class PictureConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prepareSegmentationModule: PrepareSegmentationModuleUseCase = mockk(relaxed = true)
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private fun createViewModel(
        source: PictureConfirmSource = PictureConfirmSource.CAMERA,
        returnResultOnly: Boolean = false,
    ) = PictureConfirmViewModel(
        uri = URI,
        source = source,
        returnResultOnly = returnResultOnly,
        prepareSegmentationModuleUseCase = prepareSegmentationModule,
        analyticsLogger = analyticsLogger,
    )

    @Test
    fun init_notReturnResultOnly_preparesSegmentation() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel(returnResultOnly = false)
        runCurrent()

        coVerify(exactly = 1) { prepareSegmentationModule() }
    }

    @Test
    fun init_returnResultOnly_doesNotPrepare() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel(returnResultOnly = true)
        runCurrent()

        coVerify(exactly = 0) { prepareSegmentationModule() }
    }

    @Test
    fun retake_camera_logsAndNavigatesBack() = runTest {
        val viewModel = createViewModel(source = PictureConfirmSource.CAMERA)

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickRetake)

            assertEquals(PictureConfirmEffect.NavigateBack, awaitItem())
        }
        verify(exactly = 1) {
            analyticsLogger.logEvent(AnalyticsEvent(type = AnalyticsEventType.CANVAS_CAMERA_RETAKE_TAPPED))
        }
    }

    @Test
    fun retake_gallery_navigatesBackWithoutLog() = runTest {
        val viewModel = createViewModel(source = PictureConfirmSource.GALLERY)

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickRetake)

            assertEquals(PictureConfirmEffect.NavigateBack, awaitItem())
        }
        verify(exactly = 0) { analyticsLogger.logEvent(any()) }
    }

    @Test
    fun confirm_returnResultOnly_returnsResult() = runTest {
        val viewModel = createViewModel(source = PictureConfirmSource.GALLERY, returnResultOnly = true)

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickConfirm)

            assertEquals(
                PictureConfirmEffect.ReturnResult(uri = URI, source = PictureConfirmSource.GALLERY),
                awaitItem(),
            )
        }
    }

    @Test
    fun confirm_otherwise_navigatesToSegmentation() = runTest {
        val viewModel = createViewModel(returnResultOnly = false)

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickConfirm)

            assertEquals(PictureConfirmEffect.NavigateToSegmentation(uri = URI), awaitItem())
        }
    }

    @Test
    fun close_showsQuitDialog_thenConfirmQuits() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickClose)
            assertTrue(viewModel.state.value.showQuitDialog)

            viewModel.processIntent(PictureConfirmIntent.OnConfirmQuit)

            assertFalse(viewModel.state.value.showQuitDialog)
            assertEquals(PictureConfirmEffect.QuitToCanvas, awaitItem())
        }
    }

    @Test
    fun close_thenDismiss_hidesQuitDialogWithoutEffect() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(PictureConfirmIntent.OnClickClose)
            assertTrue(viewModel.state.value.showQuitDialog)

            viewModel.processIntent(PictureConfirmIntent.OnDismissQuit)

            assertFalse(viewModel.state.value.showQuitDialog)
            expectNoEvents()
        }
    }
}
