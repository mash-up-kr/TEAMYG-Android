package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveType
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasShareMedia
import com.teamyg.parfait.feature.groups.canvas.impl.model.PreviewAction
import com.teamyg.parfait.feature.groups.canvas.impl.model.RecordPhase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File

internal data class CanvasImageSaveUiState(
    val recordPhase: RecordPhase = RecordPhase.IDLE,
    /** 저장·공유를 눌러 잠긴 상태 — 버튼과 닫기를 막는다 */
    val isBusy: Boolean = false,
) : UiState {
    val isRecording: Boolean get() = recordPhase == RecordPhase.RECORDING

    /**
     * 녹화 효과가 mp4 를 구워야 하는지. 녹화 중에 효과가 다시 돌면 이전 녹화는 취소돼 부분 파일이
     * 지워졌으므로 처음부터 다시 굽는다
     */
    val needsRecording: Boolean get() = recordPhase != RecordPhase.FINISHED
}

internal sealed interface CanvasImageSaveIntent : UiIntent {
    data object StartRecording : CanvasImageSaveIntent

    data class RecordingSucceeded(
        val file: File,
    ) : CanvasImageSaveIntent

    data object RecordingFailed : CanvasImageSaveIntent

    data object RequestSave : CanvasImageSaveIntent

    data object RequestShare : CanvasImageSaveIntent
}

internal sealed interface CanvasImageSaveEffect : UiSideEffect {
    data class DispatchSave(
        val result: CanvasSaveResult,
    ) : CanvasImageSaveEffect

    data class DispatchShare(
        val file: File,
        val media: CanvasShareMedia,
    ) : CanvasImageSaveEffect

    /** 영상을 만들지 못해 공유로 넘기지 못했다 */
    data object DispatchShareFailed : CanvasImageSaveEffect
}

/**
 * 저장 미리보기의 상태 기계. 무엇을 언제 내보내고 파일을 지킬지/지울지의 판단을 전부 여기 모은다.
 * 사진·영상 분기도 여기서 한다 — 녹화를 기다리는 건 영상뿐이고, 사진은 누르는 즉시 내보낸다
 */
@HiltViewModel(assistedFactory = CanvasImageSaveViewModel.Factory::class)
internal class CanvasImageSaveViewModel
@AssistedInject
constructor(
    @Assisted private val saveType: CanvasSaveType,
    @Assisted private val imagePath: String,
) : BaseViewModel<CanvasImageSaveUiState, CanvasImageSaveIntent, CanvasImageSaveEffect>(
    CanvasImageSaveUiState(),
) {
    @AssistedFactory
    interface Factory {
        fun create(
            saveType: CanvasSaveType,
            imagePath: String,
        ): CanvasImageSaveViewModel
    }

    private var pendingAction: PreviewAction? = null
    private var recordedFile: File? = null

    // 이 화면은 효과 하나를 내보내고 닫힌다. 연타로 두 번 내보내면 백스택이 한 칸 더 빠진다
    private var dispatched = false

    override fun processIntent(intent: CanvasImageSaveIntent) {
        when (intent) {
            CanvasImageSaveIntent.StartRecording -> {
                // 다 구운 뒤 녹화 효과가 다시 돌아도(구성 변경) 같은 파일을 덮어 굽지 않는다
                if (state.value.recordPhase == RecordPhase.FINISHED) return
                updateState { copy(recordPhase = RecordPhase.RECORDING) }
            }

            is CanvasImageSaveIntent.RecordingSucceeded -> {
                recordedFile = intent.file
                finishRecording()
            }

            CanvasImageSaveIntent.RecordingFailed -> {
                // 실패한 녹화는 부분 파일을 지운다(recordCanvasVideo). 지워진 경로를 저장·공유로 넘기지 않는다
                recordedFile = null
                finishRecording()
            }

            CanvasImageSaveIntent.RequestSave -> request(PreviewAction.SAVE)

            CanvasImageSaveIntent.RequestShare -> request(PreviewAction.SHARE)
        }
    }

    // 먼저 누른 동작이 이긴다. 한 프레임에 저장·공유가 겹쳐 와도 뒤의 것이 앞을 덮지 않는다
    private fun request(action: PreviewAction) {
        if (pendingAction != null) return
        pendingAction = action
        updateState { copy(isBusy = true) }
        dispatchIfReady()
    }

    private fun finishRecording() {
        updateState { copy(recordPhase = RecordPhase.FINISHED) }
        dispatchIfReady()
    }

    // 영상이 아직 녹화 중이면 넘어가고, [finishRecording] 이 다시 불러 깨운다
    private fun dispatchIfReady() {
        if (dispatched) return
        val action = pendingAction ?: return
        val effect = when (saveType) {
            CanvasSaveType.IMAGE -> imageEffect(action)
            CanvasSaveType.VIDEO -> if (state.value.recordPhase == RecordPhase.FINISHED) videoEffect(action) else return
        }
        dispatched = true
        postSideEffect(effect)
    }

    private fun imageEffect(action: PreviewAction): CanvasImageSaveEffect = when (action) {
        PreviewAction.SAVE -> CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.Image(imagePath = imagePath))
        PreviewAction.SHARE -> CanvasImageSaveEffect.DispatchShare(File(imagePath), CanvasShareMedia.IMAGE)
    }

    private fun videoEffect(action: PreviewAction): CanvasImageSaveEffect {
        val file = recordedFile
        return when (action) {
            PreviewAction.SAVE ->
                if (file != null) {
                    CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.Video(videoPath = file.absolutePath))
                } else {
                    CanvasImageSaveEffect.DispatchSave(CanvasSaveResult.VideoFailed)
                }

            PreviewAction.SHARE ->
                if (file != null) {
                    CanvasImageSaveEffect.DispatchShare(file, CanvasShareMedia.VIDEO)
                } else {
                    CanvasImageSaveEffect.DispatchShareFailed
                }
        }
    }

    /**
     * 나갈 때 지울 파일 — 저장·공유로 넘기지 않은 미리 구운 mp4만.
     * mp4 가 있는 채로 내보낸 효과는 늘 그 파일을 넘기므로, 내보냈는지만 보면 된다.
     *
     * State 가 아니라 질의인 이유: 화면에 그릴 값이 아니고, `onDispose` 가 그 순간 동기로 읽어야 한다
     */
    fun fileToDeleteOnExit(): File? = recordedFile?.takeIf { dispatched.not() }
}
