package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import java.io.File

/**
 * 미리 굽기 진행 단계. 녹화 루프는 화면 계층(Route)이 돌리고 결과만 이 홀더로 보고한다.
 */
internal enum class RecordPhase { IDLE, RECORDING, FINISHED }

private enum class PreviewAction { SAVE, SHARE }

/** 미리보기에서 내보낼 것. Route 가 실제 결과 전송·공유·네비게이션으로 옮긴다 */
internal sealed interface PreviewOutcome {
    /** 저장 결과를 캔버스 메인에 보낸다 */
    data class Save(val result: CanvasSaveResult) : PreviewOutcome

    /** 이 파일을 인스타그램 스토리로 공유한다 */
    data class Share(val file: File) : PreviewOutcome

    /** 공유 실패 등 — 조용히 닫기만 한다 */
    data object Back : PreviewOutcome
}

/**
 * 영상 저장 미리보기의 상태 기계. 무엇을 언제 내보내고 파일을 지킬지/지울지의 판단은 전부 여기 모은다
 * 종료 경로가 늘어도 삭제·전송 조건을 한 곳에서 검증한다.
 */
internal class CanvasVideoSaveState {
    var phase by mutableStateOf(RecordPhase.IDLE)
        private set

    private var pendingAction by mutableStateOf<PreviewAction?>(null)

    private var recordedFile: File? = null
    private var consumed = false
    private var dispatched = false

    /** 녹화 레이어를 컴포지션에 붙여야 하는 동안 */
    val isRecording: Boolean get() = phase == RecordPhase.RECORDING

    /** 저장·공유를 눌러 잠긴 상태 — 버튼과 닫기를 막는다. [phase] 와 함께 dispatch 를 깨우는 신호다 */
    val isBusy: Boolean get() = pendingAction != null

    fun startRecording() {
        phase = RecordPhase.RECORDING
    }

    fun recordingSucceeded(file: File) {
        recordedFile = file
        phase = RecordPhase.FINISHED
    }

    // 실패는 별도 단계가 아니라 "끝났는데 파일이 없음"이다
    fun recordingFailed() {
        phase = RecordPhase.FINISHED
    }

    fun requestSave() {
        pendingAction = PreviewAction.SAVE
    }

    fun requestShare() {
        pendingAction = PreviewAction.SHARE
    }

    /**
     * 누른 동작과 mp4 가 둘 다 준비되면 내보낼 것을 돌려준다. 아직 녹화 중이면 null 이라
     * 호출부는 기다렸다 상태가 바뀌면 다시 묻는다. 저장·공유로 파일을 넘기면 [consumed] 로 표시해
     * 종료 삭제에서 뺀다. 한 번 내보낸 뒤로는 null 만 돌려 재전송을 막는다.
     */
    fun dispatch(): PreviewOutcome? {
        if (dispatched) return null
        val action = pendingAction ?: return null
        if (phase != RecordPhase.FINISHED) return null

        val file = recordedFile
        val outcome = when (action) {
            PreviewAction.SAVE ->
                if (file != null) {
                    consumed = true
                    PreviewOutcome.Save(CanvasSaveResult.Video(videoPath = file.absolutePath))
                } else {
                    PreviewOutcome.Save(CanvasSaveResult.VideoFailed)
                }

            PreviewAction.SHARE ->
                if (file != null) {
                    consumed = true
                    PreviewOutcome.Share(file)
                } else {
                    PreviewOutcome.Back
                }
        }
        dispatched = true
        return outcome
    }

    /** 나갈 때 지울 파일 — 저장·공유로 소비되지 않은 미리 구운 mp4만. 없으면 null */
    fun fileToDeleteOnExit(): File? = recordedFile?.takeIf { !consumed }
}
