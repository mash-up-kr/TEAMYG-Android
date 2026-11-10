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
    fun share_keepsFileOnlyAfterMarkShared() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingSucceeded(file)
        state.requestShare()

        assertEquals(PreviewOutcome.Share(file), state.dispatch())
        // 실제로 인스타를 띄웠다는 보고(markShared) 전엔 아직 소비 아님 → 나가면 지운다
        assertEquals(file, state.fileToDeleteOnExit())

        state.markShared()
        assertNull(state.fileToDeleteOnExit())
    }

    @Test
    fun share_whenCancelled_fileStaysDeletableAndCanRetry() {
        val state = CanvasVideoSaveState()
        state.startRecording()
        state.recordingSucceeded(file)
        state.requestShare()
        state.dispatch()

        // 공유 실패 → 되돌리면 버튼이 다시 열리고(busy 아님) 파일은 소비 안 돼 삭제 대상이다
        state.cancelPendingAction()
        assertFalse(state.isBusy)
        assertEquals(file, state.fileToDeleteOnExit())

        // 재시도 — 다시 요청하면 dispatch 가 또 Share 를 돌려준다
        state.requestShare()
        assertEquals(PreviewOutcome.Share(file), state.dispatch())
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
