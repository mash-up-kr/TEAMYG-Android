package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasShareMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

internal enum class RecordPhase { IDLE, RECORDING, FINISHED }

private enum class PreviewAction { SAVE, SHARE }

internal data class CanvasImageSaveUiState(
    /** 녹화 레이어를 컴포지션에 붙여야 하는 동안 */
    val isRecording: Boolean = false,
    /** 저장·공유를 눌러 잠긴 상태 — 버튼과 닫기를 막는다 */
    val isBusy: Boolean = false,
) : UiState

internal sealed interface CanvasImageSaveIntent : UiIntent {
    data object StartRecording : CanvasImageSaveIntent

    data class RecordingSucceeded(
        val file: File,
    ) : CanvasImageSaveIntent

    data object RecordingFailed : CanvasImageSaveIntent

    data object RequestSave : CanvasImageSaveIntent

    data object RequestShare : CanvasImageSaveIntent

    /** 사진 미리보기의 공유. 녹화를 기다릴 것 없이 캡처한 PNG 를 바로 넘긴다 */
    data class RequestImageShare(
        val imagePath: String,
    ) : CanvasImageSaveIntent
}

/** 미리보기에서 내보낼 것. Route 가 실제 결과 전송·공유·네비게이션으로 옮긴다 */
internal sealed interface CanvasImageSaveEffect : UiSideEffect {
    /** 저장 결과를 캔버스 메인에 보낸다 */
    data class DispatchSave(
        val result: CanvasSaveResult,
    ) : CanvasImageSaveEffect

    /** 이 파일을 인스타그램 스토리로 공유한다 */
    data class DispatchShare(
        val file: File,
        val media: CanvasShareMedia,
    ) : CanvasImageSaveEffect

    /** 공유 실패 등 — 조용히 닫기만 한다 */
    data object DispatchBack : CanvasImageSaveEffect
}

/**
 * 저장 미리보기의 상태 기계. 무엇을 언제 내보내고 파일을 지킬지/지울지의 판단을 전부 여기 모은다.
 * 녹화를 기다리는 건 영상뿐이고, 사진 공유는 바로 내보낸다
 */
@HiltViewModel
internal class CanvasImageSaveViewModel
@Inject
constructor() : BaseViewModel<CanvasImageSaveUiState, CanvasImageSaveIntent, CanvasImageSaveEffect>(
    CanvasImageSaveUiState(),
) {
    private var phase = RecordPhase.IDLE
    private var pendingAction: PreviewAction? = null
    private var recordedFile: File? = null
    private var consumed = false
    private var dispatched = false

    override fun processIntent(intent: CanvasImageSaveIntent) {
        when (intent) {
            CanvasImageSaveIntent.StartRecording -> {
                phase = RecordPhase.RECORDING
                updateState { copy(isRecording = true) }
            }

            is CanvasImageSaveIntent.RecordingSucceeded -> {
                recordedFile = intent.file
                finishRecording()
            }

            CanvasImageSaveIntent.RecordingFailed -> finishRecording()

            CanvasImageSaveIntent.RequestSave -> {
                pendingAction = PreviewAction.SAVE
                updateState { copy(isBusy = true) }
                dispatchIfReady()
            }

            CanvasImageSaveIntent.RequestShare -> {
                pendingAction = PreviewAction.SHARE
                updateState { copy(isBusy = true) }
                dispatchIfReady()
            }

            is CanvasImageSaveIntent.RequestImageShare -> {
                updateState { copy(isBusy = true) }
                dispatchOnce(CanvasImageSaveEffect.DispatchShare(File(intent.imagePath), CanvasShareMedia.IMAGE))
            }
        }
    }

    private fun finishRecording() {
        phase = RecordPhase.FINISHED
        updateState { copy(isRecording = false) }
        dispatchIfReady()
    }

    /**
     * 누른 동작과 mp4 가 둘 다 준비되면 효과로 내보낸다. 아직 녹화 중이면 아무 일도 하지 않고,
     * 나중에 [finishRecording] 이 다시 불러 깨운다. 저장·공유로 파일을 넘기면 [consumed] 로 표시해
     * 종료 삭제에서 뺀다. 한 번 내보낸 뒤로는 다시 부르지 않는다.
     */
    private fun dispatchIfReady() {
        if (dispatched) return
        val action = pendingAction ?: return
        if (phase != RecordPhase.FINISHED) return

        val file = recordedFile
        val effect = when (action) {
            PreviewAction.SAVE ->
                if (file != null) {
                    consumed = true
                    CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.Video(videoPath = file.absolutePath))
                } else {
                    CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.VideoFailed)
                }

            PreviewAction.SHARE ->
                if (file != null) {
                    consumed = true
                    CanvasImageSaveEffect.DispatchShare(file, CanvasShareMedia.VIDEO)
                } else {
                    CanvasImageSaveEffect.DispatchBack
                }
        }
        dispatchOnce(effect)
    }

    /** 이 화면은 효과 하나를 내보내고 닫힌다. 연타로 두 번 내보내면 백스택이 한 칸 더 빠진다 */
    private fun dispatchOnce(effect: CanvasImageSaveEffect) {
        if (dispatched) return
        dispatched = true
        postSideEffect(effect)
    }

    /** 나갈 때 지울 파일 — 저장·공유로 소비되지 않은 미리 구운 mp4만. 없으면 null */
    fun fileToDeleteOnExit(): File? = recordedFile?.takeIf { !consumed }
}
