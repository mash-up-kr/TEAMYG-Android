package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.model.SubjectCoverage
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveBitmapUseCase
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.UndoRedoStack
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.measureSubject
import com.teamyg.parfait.feature.segmentation.impl.editor.trimTo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ToppingEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val mode: ToppingEditMode = ToppingEditMode.ERASE,
    val brushWidthDp: Float = DEFAULT_BRUSH_WIDTH_DP,
    val areaHistory: UndoRedoStack<ToppingEditStroke> = UndoRedoStack(),
    val isSaving: Boolean = false,
    val showQuitDialog: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    /** 확정된 획. 캔버스와 저장이 함께 본다 */
    val strokes: List<ToppingEditStroke> get() = areaHistory.done

    val minBrushWidthDp: Float get() = MIN_BRUSH_WIDTH_DP

    val maxBrushWidthDp: Float get() = MAX_BRUSH_WIDTH_DP

    companion object {
        /**
         * 화면에 보이는 붓 굵기. 사진 해상도나 기기 밀도가 달라도 체감 굵기가 같도록 dp 로 잡는다.
         * 획을 확정할 때 화면이 원본 비트맵 좌표계 굵기로 환산한다.
         */
        private const val DEFAULT_BRUSH_WIDTH_DP = 10f
        private const val MIN_BRUSH_WIDTH_DP = 2f
        private const val MAX_BRUSH_WIDTH_DP = 50f
    }
}

sealed interface ToppingEditIntent : UiIntent {
    data class ChangeMode(val mode: ToppingEditMode) : ToppingEditIntent

    data class ChangeBrushWidth(val width: Float) : ToppingEditIntent

    /** 드래그가 끝난 획을 확정한다. 그리는 도중의 획은 화면이 들고 있는다 */
    data class AddStroke(val stroke: ToppingEditStroke) : ToppingEditIntent

    data object UndoArea : ToppingEditIntent

    data object RedoArea : ToppingEditIntent

    data object ClickDone : ToppingEditIntent

    /** 닫기 버튼 */
    data object ClickClose : ToppingEditIntent

    data object ConfirmQuit : ToppingEditIntent

    data object DismissQuit : ToppingEditIntent
}

sealed interface ToppingEditEffect : UiSideEffect {
    data object LoadFailed : ToppingEditEffect

    data object SaveFailed : ToppingEditEffect

    /**
     * 남은 영역이 하한에 못 미쳐 저장하지 않았다.
     *
     * [SaveFailed] 와 합치지 않는 이유는 사용자가 할 일이 다르기 때문이다 — 저장 실패는 재시도이고
     * 이쪽은 되돌리기다.
     */
    data object SubjectTooSmall : ToppingEditEffect

    /** 초안 기록을 마쳐 토핑 배치(C-106)로 간다 */
    data object GoToPlace : ToppingEditEffect

    /**
     * 초안을 기록하지 못해 C-106 으로 이어 갈 수 없다.
     *
     * [SaveFailed] 와 합치지 않는 이유는 재시도로 풀리지 않아 캔버스에서 다시 시작해야 하기 때문이다.
     */
    data object DraftUnavailable : ToppingEditEffect

    data object QuitToCanvas : ToppingEditEffect
}

@HiltViewModel(assistedFactory = ToppingEditViewModel.Factory::class)
class ToppingEditViewModel
@AssistedInject constructor(
    @Assisted("sourceImageUri") private val sourceImageUri: String,
    @Assisted("segmentationImageUri") private val segmentationImageUri: String,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val saveBitmapUseCase: SaveBitmapUseCase,
    private val recordToppingDraft: RecordToppingDraftUseCase,
) : BaseViewModel<ToppingEditState, ToppingEditIntent, ToppingEditEffect>(ToppingEditState()) {
    /** 그만두기를 확정했다. 연타해도 캔버스로 나가는 이동은 한 번만 보낸다 */
    private var isQuitConfirmed = false

    init {
        loadImages()
    }

    override fun processIntent(intent: ToppingEditIntent) {
        when (intent) {
            is ToppingEditIntent.ChangeMode -> {
                updateState { copy(mode = intent.mode) }
            }

            is ToppingEditIntent.ChangeBrushWidth -> {
                updateState { copy(brushWidthDp = intent.width.coerceIn(minBrushWidthDp, maxBrushWidthDp)) }
            }

            is ToppingEditIntent.AddStroke -> {
                updateState { copy(areaHistory = areaHistory.push(intent.stroke)) }
            }

            ToppingEditIntent.UndoArea -> {
                updateState { copy(areaHistory = areaHistory.undo()) }
            }

            ToppingEditIntent.RedoArea -> {
                updateState { copy(areaHistory = areaHistory.redo()) }
            }

            ToppingEditIntent.ClickDone -> completeEdit()

            ToppingEditIntent.ClickClose -> updateState { copy(showQuitDialog = true) }

            ToppingEditIntent.DismissQuit -> updateState { copy(showQuitDialog = false) }

            ToppingEditIntent.ConfirmQuit -> {
                if (isQuitConfirmed) return

                isQuitConfirmed = true
                // 화면이 걷히는 전환 동안 팝업이 캔버스 위에 남지 않게 닫는다
                updateState { copy(showQuitDialog = false) }
                postSideEffect(ToppingEditEffect.QuitToCanvas)
            }
        }
    }

    private fun loadImages() {
        viewModelScope.launch {
            val originBitmap = decodeBitmapOrNull(sourceImageUri)
            // 두 비트맵은 읽기만 하고 따로 recycle 하지 않아 같은 주소면 하나를 함께 쓴다
            val segmentationBitmap = if (segmentationImageUri == sourceImageUri) {
                originBitmap
            } else {
                decodeBitmapOrNull(segmentationImageUri)
            }

            if (originBitmap == null || segmentationBitmap == null) {
                postSideEffect(ToppingEditEffect.LoadFailed)
                return@launch
            }

            updateState { copy(originBitmap = originBitmap, segmentationBitmap = segmentationBitmap) }
        }
    }

    private suspend fun decodeBitmapOrNull(uri: String): Bitmap? =
        (decodeImageUseCase(uri).getOrNull() as? AndroidBitmap)?.getRawData()

    private fun completeEdit() {
        val current = state.value
        val originBitmap = current.originBitmap ?: return
        val segmentationBitmap = current.segmentationBitmap ?: return
        if (current.isSaving) return

        // 코루틴이 돌기 전에 올린다 — 안에서 올리면 시작을 기다리는 사이의 연타가 위 가드를 지난다
        updateState { copy(isSaving = true) }

        launch(onError = { finishSaving(ToppingEditEffect.SaveFailed) }) {
            val (cutout, measure) = withContext(Dispatchers.Default) {
                val built = buildCutoutBitmap(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    strokes = current.strokes,
                )
                built to built.measureSubject()
            }

            // 파일을 쓰기 전에 판정한다 — 뒤로 미루면 쓸모없는 캐시 파일 두 장이 남는다
            if (!SubjectCoverage.isLargeEnough(
                    alphaSum = measure.alphaSum,
                    canvasArea = cutout.width.toLong() * cutout.height,
                )
            ) {
                cutout.recycle()
                finishSaving(ToppingEditEffect.SubjectTooSmall)
                return@launch
            }

            // cutout 은 재편집 좌표계를 지키려고 원본 크기를 유지해야 하고, 보여 주고 올릴 알맹이는
            // 투명 여백 없이 실제 토핑 크기여야 한다. 여백이 붙은 채로 올라가면 배치 좌표가 어긋난다
            val trimmedCutout = withContext(Dispatchers.Default) { cutout.trimTo(measure) }

            // cutout 이 원본 좌표계를 유지한 판이라 긴 변이 그대로 원본 사진의 긴 변이다
            val sourceLongSide = maxOf(cutout.width, cutout.height)

            // 화면 사이에서는 비트맵 대신 경로를 주고받으므로 여기서 파일로 떨군다.
            // 저장 전용으로 만든 비트맵이라 화면이 잡고 있지 않고, 원본 해상도라 수십 MB 에
            // 이르기도 해서 파일로 떨구는 즉시 메모리를 돌려준다
            val (cutoutPath, subjectPath) = try {
                val savedCutoutPath = saveBitmapUseCase(cutout.toAndroidBitmap()).getOrNull()
                val savedSubjectPath = saveBitmapUseCase(trimmedCutout.toAndroidBitmap()).getOrNull()
                savedCutoutPath to savedSubjectPath
            } finally {
                if (trimmedCutout !== cutout) trimmedCutout.recycle()
                cutout.recycle()
            }

            if (cutoutPath == null || subjectPath == null) {
                finishSaving(ToppingEditEffect.SaveFailed)
                return@launch
            }

            val result = ToppingEditResult(
                subjectImagePath = subjectPath,
                cutoutImagePath = cutoutPath,
                sourceLongSide = SourceLongSide(sourceLongSide),
            )
            finishSaving(completionEffect(result))
        }
    }

    private suspend fun completionEffect(result: ToppingEditResult): ToppingEditEffect =
        if (recordToppingDraft.recordEditResult(result)) {
            ToppingEditEffect.GoToPlace
        } else {
            ToppingEditEffect.DraftUnavailable
        }

    /**
     * 저장과 기록 사이에서 내리면 그 틈의 완료 탭이 한 번 더 저장하므로 끝난 뒤 한 번만 내린다.
     * 토핑 배치(C-106)로 갈 때도 내리는 것은 이 화면이 백스택에 남아, 켠 채 나가면 돌아왔을 때 갇히기 때문이다.
     */
    private fun finishSaving(effect: ToppingEditEffect) {
        updateState { copy(isSaving = false) }
        postSideEffect(effect)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sourceImageUri") sourceImageUri: String,
            @Assisted("segmentationImageUri") segmentationImageUri: String,
        ): ToppingEditViewModel
    }
}
