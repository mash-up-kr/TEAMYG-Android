package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasShareMedia
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class CanvasImageSaveViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val file = File("canvas_timelapse.mp4")

    @Test
    fun recordingSucceeded_thenSave_returnsVideoAndKeepsFile() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
            viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))
            viewModel.processIntent(CanvasImageSaveIntent.RequestSave)

            assertEquals(
                CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.Video(file.absolutePath)),
                awaitItem(),
            )
        }
        // 저장으로 넘겼으니 나갈 때 지우지 않는다
        assertNull(viewModel.fileToDeleteOnExit())
    }

    @Test
    fun recordingSucceeded_noAction_deletesFileOnExit() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()
        viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
        viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))

        // 저장·공유 없이 그냥 나가면 미리 구운 mp4 를 지운다
        assertEquals(file, viewModel.fileToDeleteOnExit())
    }

    @Test
    fun save_whileStillRecording_waitsThenDispatches() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
            viewModel.processIntent(CanvasImageSaveIntent.RequestSave)

            // 아직 녹화 중이라 내보낼 게 없다
            expectNoEvents()

            viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))
            assertEquals(
                CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.Video(file.absolutePath)),
                awaitItem(),
            )
        }
    }

    @Test
    fun recordingFailed_thenSave_returnsVideoFailedAndNothingToDelete() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            viewModel.processIntent(CanvasImageSaveIntent.RequestSave)

            assertEquals(CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.VideoFailed), awaitItem())
        }
        assertNull(viewModel.fileToDeleteOnExit())
    }

    @Test
    fun recordingSucceeded_thenShare_returnsShareAndKeepsFile() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
            viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))
            viewModel.processIntent(CanvasImageSaveIntent.RequestShare)

            assertEquals(CanvasImageSaveEffect.DispatchShare(file, CanvasShareMedia.VIDEO), awaitItem())
        }
        // 공유로 넘겼으니 나갈 때 지우지 않는다
        assertNull(viewModel.fileToDeleteOnExit())
    }

    @Test
    fun share_whenRecordingFailed_returnsBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            viewModel.processIntent(CanvasImageSaveIntent.RequestShare)

            assertEquals(CanvasImageSaveEffect.DispatchBack, awaitItem())
        }
    }

    @Test
    fun dispatch_onlyFiresOnce() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))
            viewModel.processIntent(CanvasImageSaveIntent.RequestSave)
            assertTrue(awaitItem() is CanvasImageSaveEffect.DispatchSave)

            // 같은 요청을 다시 보내도 — 이미 내보냈으니 재전송하지 않는다
            viewModel.processIntent(CanvasImageSaveIntent.RequestSave)
            expectNoEvents()
        }
    }

    @Test
    fun imageShare_dispatchesImageShareWithoutRecording() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.RequestImageShare("canvas_capture.png"))

            // 사진은 녹화를 기다리지 않고 바로 내보낸다
            assertEquals(
                CanvasImageSaveEffect.DispatchShare(File("canvas_capture.png"), CanvasShareMedia.IMAGE),
                awaitItem(),
            )
        }
        assertTrue(viewModel.state.value.isBusy)
    }

    @Test
    fun imageShare_doubleTap_dispatchesOnce() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasImageSaveIntent.RequestImageShare("canvas_capture.png"))
            assertTrue(awaitItem() is CanvasImageSaveEffect.DispatchShare)

            // 연타로 다시 와도 내보내지 않는다 — 두 번 내보내면 백스택이 한 칸 더 빠진다
            viewModel.processIntent(CanvasImageSaveIntent.RequestImageShare("canvas_capture.png"))
            expectNoEvents()
        }
    }

    @Test
    fun uiState_reflectsRecordingAndBusyFlags() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = CanvasImageSaveViewModel()
        assertFalse(viewModel.state.value.isRecording)
        assertFalse(viewModel.state.value.isBusy)

        viewModel.processIntent(CanvasImageSaveIntent.StartRecording)
        assertTrue(viewModel.state.value.isRecording)

        viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(file))
        assertFalse(viewModel.state.value.isRecording)

        viewModel.processIntent(CanvasImageSaveIntent.RequestSave)
        assertTrue(viewModel.state.value.isBusy)
    }
}
