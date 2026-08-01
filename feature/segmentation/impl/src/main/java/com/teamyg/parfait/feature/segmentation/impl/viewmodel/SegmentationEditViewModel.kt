package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.feature.segmentation.impl.editor.SegmentationEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.SegmentationEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.SegmentationEditTab
import com.teamyg.parfait.feature.segmentation.impl.editor.buildEditedBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 원본 긴 변 대비 붓 굵기 비율. 이미지 해상도가 달라도 체감 굵기가 비슷하도록 비율로 잡는다 */
private const val DEFAULT_BRUSH_WIDTH_RATIO = 0.04f
private const val MIN_BRUSH_WIDTH_RATIO = 0.01f
private const val MAX_BRUSH_WIDTH_RATIO = 0.15f

data class SegmentationEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val tab: SegmentationEditTab = SegmentationEditTab.AREA,
    val mode: SegmentationEditMode = SegmentationEditMode.ERASE,
    val brushWidth: Float = 0f,
    val strokes: List<SegmentationEditStroke> = emptyList(),
    val redoableStrokes: List<SegmentationEditStroke> = emptyList(),
    val isSaving: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    val canUndo: Boolean get() = strokes.isNotEmpty()

    val canRedo: Boolean get() = redoableStrokes.isNotEmpty()

    val minBrushWidth: Float get() = brushWidthRatioOf(MIN_BRUSH_WIDTH_RATIO)

    val maxBrushWidth: Float get() = brushWidthRatioOf(MAX_BRUSH_WIDTH_RATIO)

    private fun brushWidthRatioOf(ratio: Float): Float {
        val bitmap = originBitmap ?: return 0f
        return maxOf(bitmap.width, bitmap.height) * ratio
    }
}

sealed interface SegmentationEditIntent : UiIntent {
    data class ChangeTab(val tab: SegmentationEditTab) : SegmentationEditIntent

    data class ChangeMode(val mode: SegmentationEditMode) : SegmentationEditIntent

    data class ChangeBrushWidth(val width: Float) : SegmentationEditIntent

    /** 드래그가 끝난 획을 확정한다. 그리는 도중의 획은 화면이 들고 있는다 */
    data class AddStroke(val stroke: SegmentationEditStroke) : SegmentationEditIntent

    data object Undo : SegmentationEditIntent

    data object Redo : SegmentationEditIntent

    data object Reset : SegmentationEditIntent

    data object ClickDone : SegmentationEditIntent
}

sealed interface SegmentationEditEffect : UiSideEffect {
    data object LoadFailed : SegmentationEditEffect

    data class EditCompleted(val editedBitmap: Bitmap) : SegmentationEditEffect
}

@HiltViewModel(assistedFactory = SegmentationEditViewModel.Factory::class)
class SegmentationEditViewModel
@AssistedInject constructor(
    @Assisted("sourceImageUri") private val sourceImageUri: String,
    @Assisted("segmentationImageUri") private val segmentationImageUri: String,
    private val decodeImageUseCase: DecodeImageUseCase,
) : BaseViewModel<SegmentationEditState, SegmentationEditIntent, SegmentationEditEffect>(
    initialState = SegmentationEditState(),
) {
    init {
        loadImages()
    }

    override fun processIntent(intent: SegmentationEditIntent) {
        when (intent) {
            is SegmentationEditIntent.ChangeTab -> {
                updateState { copy(tab = intent.tab) }
            }

            is SegmentationEditIntent.ChangeMode -> {
                updateState { copy(mode = intent.mode) }
            }

            is SegmentationEditIntent.ChangeBrushWidth -> {
                updateState { copy(brushWidth = intent.width.coerceIn(minBrushWidth, maxBrushWidth)) }
            }

            is SegmentationEditIntent.AddStroke -> {
                // 새로 그리면 redo 는 무효가 된다
                updateState { copy(strokes = strokes + intent.stroke, redoableStrokes = emptyList()) }
            }

            SegmentationEditIntent.Undo -> {
                updateState {
                    val last = strokes.lastOrNull() ?: return@updateState this
                    copy(
                        strokes = strokes.dropLast(1),
                        redoableStrokes = redoableStrokes + last,
                    )
                }
            }

            SegmentationEditIntent.Redo -> {
                updateState {
                    val last = redoableStrokes.lastOrNull() ?: return@updateState this
                    copy(
                        strokes = strokes + last,
                        redoableStrokes = redoableStrokes.dropLast(1),
                    )
                }
            }

            SegmentationEditIntent.Reset -> {
                updateState { copy(strokes = emptyList(), redoableStrokes = emptyList()) }
            }

            SegmentationEditIntent.ClickDone -> completeEdit()
        }
    }

    private fun loadImages() {
        viewModelScope.launch {
            val originBitmap = decodeBitmapOrNull(sourceImageUri)
            val segmentationBitmap = decodeBitmapOrNull(segmentationImageUri)

            if (originBitmap == null || segmentationBitmap == null) {
                postSideEffect(SegmentationEditEffect.LoadFailed)
                return@launch
            }

            updateState {
                copy(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    brushWidth = maxOf(originBitmap.width, originBitmap.height) * DEFAULT_BRUSH_WIDTH_RATIO,
                )
            }
        }
    }

    private suspend fun decodeBitmapOrNull(uri: String): Bitmap? = runCatching {
        (decodeImageUseCase(uri) as? AndroidBitmap)?.getRawData()
    }.getOrNull()

    private fun completeEdit() {
        val current = state.value
        val originBitmap = current.originBitmap ?: return
        val segmentationBitmap = current.segmentationBitmap ?: return
        if (current.isSaving) return

        viewModelScope.launch {
            updateState { copy(isSaving = true) }
            val edited = withContext(Dispatchers.Default) {
                buildEditedBitmap(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    strokes = current.strokes,
                )
            }
            updateState { copy(isSaving = false) }
            postSideEffect(SegmentationEditEffect.EditCompleted(edited))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sourceImageUri") sourceImageUri: String,
            @Assisted("segmentationImageUri") segmentationImageUri: String,
        ): SegmentationEditViewModel
    }
}
