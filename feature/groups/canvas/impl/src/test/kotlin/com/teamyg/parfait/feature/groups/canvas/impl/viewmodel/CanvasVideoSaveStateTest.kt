package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CanvasVideoSaveStateTest {
    private val file = File("canvas_timelapse.mp4")

    @Test
    fun recordingSucceeded_thenSave_returnsVideoAndKeepsFile() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingSucceeded(file)
        state.requestSave()

        assertEquals(
            PreviewOutcome.Save(CanvasSaveResult.Video(file.absolutePath)),
            state.dispatch(),
        )
        // 저장으로 넘겼으니 나갈 때 지우지 않는다
        assertNull(state.fileToDeleteOnExit())
    }

    @Test
    fun recordingSucceeded_noAction_deletesFileOnExit() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingSucceeded(file)

        // 저장·공유 없이 그냥 나가면 미리 구운 mp4 를 지운다
        assertEquals(file, state.fileToDeleteOnExit())
    }

    @Test
    fun save_whileStillRecording_waitsThenDispatches() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.requestSave()

        // 아직 녹화 중이라 내보낼 게 없다
        assertNull(state.dispatch())

        state.recordingSucceeded(file)
        assertEquals(
            PreviewOutcome.Save(CanvasSaveResult.Video(file.absolutePath)),
            state.dispatch(),
        )
    }

    @Test
    fun recordingFailed_thenSave_returnsVideoFailedAndNothingToDelete() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingFailed()
        state.requestSave()

        assertEquals(PreviewOutcome.Save(CanvasSaveResult.VideoFailed), state.dispatch())
        assertNull(state.fileToDeleteOnExit())
    }

    @Test
    fun recordingSucceeded_thenShare_returnsShareAndKeepsFile() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingSucceeded(file)
        state.requestShare()

        assertEquals(PreviewOutcome.Share(file), state.dispatch())
        // 공유로 넘겼으니 나갈 때 지우지 않는다
        assertNull(state.fileToDeleteOnExit())
    }

    @Test
    fun share_whenRecordingFailed_returnsBack() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingFailed()
        state.requestShare()

        assertEquals(PreviewOutcome.Back, state.dispatch())
    }

    @Test
    fun dispatch_onlyReturnsOutcomeOnce() {
        val state = CanvasVideoSaveState()
        state.recordingSucceeded(file)
        state.requestSave()

        assertTrue(state.dispatch() is PreviewOutcome.Save)
        // 두 번째부터는 null — 재전송을 막는다
        assertNull(state.dispatch())
    }

    @Test
    fun flags_reflectPhaseAndAction() {
        val state = CanvasVideoSaveState()
        assertFalse(state.isRecording)
        assertFalse(state.isBusy)

        state.startRecording()
        assertTrue(state.isRecording)

        state.recordingSucceeded(file)
        assertFalse(state.isRecording)

        state.requestSave()
        assertTrue(state.isBusy)
    }
}
