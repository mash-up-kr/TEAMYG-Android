package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveEditedImageUseCase
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditTab
import com.teamyg.parfait.feature.segmentation.impl.editor.buildEditedBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 화면에 보이는 붓 굵기. 사진 해상도나 기기 밀도가 달라도 체감 굵기가 같도록 dp 로 잡는다.
 * 획을 확정할 때 화면이 원본 비트맵 좌표계 굵기로 환산한다.
 */
private const val DEFAULT_BRUSH_WIDTH_DP = 10f
private const val MIN_BRUSH_WIDTH_DP = 2f
private const val MAX_BRUSH_WIDTH_DP = 50f

data class ToppingEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val tab: ToppingEditTab = ToppingEditTab.AREA,
    val mode: ToppingEditMode = ToppingEditMode.ERASE,
    val brushWidthDp: Float = DEFAULT_BRUSH_WIDTH_DP,
    val strokes: List<ToppingEditStroke> = emptyList(),
    val redoableStrokes: List<ToppingEditStroke> = emptyList(),
    val isSaving: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    val canUndo: Boolean get() = strokes.isNotEmpty()

    val canRedo: Boolean get() = redoableStrokes.isNotEmpty()

    val minBrushWidthDp: Float get() = MIN_BRUSH_WIDTH_DP

    val maxBrushWidthDp: Float get() = MAX_BRUSH_WIDTH_DP

    /**
     * 지금 고른 모드로 [points] 를 획 하나로 묶는다.
     * 찍힌 점이 없으면 만들 획도 없어 null 이다.
     *
     * @param width 원본 비트맵 좌표계 기준 굵기. 확대 배율을 아는 화면이 dp 굵기를 환산해 넘긴다
     */
    fun strokeOrNull(
        points: List<Offset>,
        width: Float,
    ): ToppingEditStroke? = if (points.isEmpty()) {
        null
    } else {
        ToppingEditStroke(mode = mode, points = points, width = width)
    }
}

sealed interface ToppingEditIntent : UiIntent {
    data class ChangeTab(val tab: ToppingEditTab) : ToppingEditIntent

    data class ChangeMode(val mode: ToppingEditMode) : ToppingEditIntent

    data class ChangeBrushWidth(val width: Float) : ToppingEditIntent

    /** 드래그가 끝난 획을 확정한다. 그리는 도중의 획은 화면이 들고 있는다 */
    data class AddStroke(val stroke: ToppingEditStroke) : ToppingEditIntent

    data object Undo : ToppingEditIntent

    data object Redo : ToppingEditIntent

    data object ClickDone : ToppingEditIntent
}

sealed interface ToppingEditEffect : UiSideEffect {
    data object LoadFailed : ToppingEditEffect

    data object SaveFailed : ToppingEditEffect

    /** @param editedImagePath 편집 결과가 저장된 파일 경로 */
    data class EditCompleted(val editedImagePath: String) : ToppingEditEffect
}

@HiltViewModel(assistedFactory = ToppingEditViewModel.Factory::class)
class ToppingEditViewModel
@AssistedInject constructor(
    @Assisted("sourceImageUri") private val sourceImageUri: String,
    @Assisted("segmentationImageUri") private val segmentationImageUri: String,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val saveEditedImageUseCase: SaveEditedImageUseCase,
) : BaseViewModel<ToppingEditState, ToppingEditIntent, ToppingEditEffect>(
    initialState = ToppingEditState(),
) {
    init {
        loadImages()
    }

    override fun processIntent(intent: ToppingEditIntent) {
        when (intent) {
            is ToppingEditIntent.ChangeTab -> {
                updateState { copy(tab = intent.tab) }
            }

            is ToppingEditIntent.ChangeMode -> {
                updateState { copy(mode = intent.mode) }
            }

            is ToppingEditIntent.ChangeBrushWidth -> {
                updateState { copy(brushWidthDp = intent.width.coerceIn(MIN_BRUSH_WIDTH_DP, MAX_BRUSH_WIDTH_DP)) }
            }

            is ToppingEditIntent.AddStroke -> {
                // 새로 그리면 redo 는 무효가 된다
                updateState { copy(strokes = strokes + intent.stroke, redoableStrokes = emptyList()) }
            }

            ToppingEditIntent.Undo -> {
                updateState {
                    val last = strokes.lastOrNull() ?: return@updateState this
                    copy(
                        strokes = strokes.dropLast(1),
                        redoableStrokes = redoableStrokes + last,
                    )
                }
            }

            ToppingEditIntent.Redo -> {
                updateState {
                    val last = redoableStrokes.lastOrNull() ?: return@updateState this
                    copy(
                        strokes = strokes + last,
                        redoableStrokes = redoableStrokes.dropLast(1),
                    )
                }
            }

            ToppingEditIntent.ClickDone -> completeEdit()
        }
    }

    private fun loadImages() {
        viewModelScope.launch {
            val originBitmap = decodeBitmapOrNull(sourceImageUri)
            val segmentationBitmap = decodeBitmapOrNull(segmentationImageUri)

            if (originBitmap == null || segmentationBitmap == null) {
                postSideEffect(ToppingEditEffect.LoadFailed)
                return@launch
            }

            updateState {
                copy(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
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

            // 화면 사이에서는 비트맵 대신 경로를 주고받으므로 여기서 파일로 떨군다.
            // 저장 전용으로 만든 비트맵이라 화면이 잡고 있지 않고, 원본 해상도라 수십 MB 에
            // 이르기도 해서 파일로 떨구는 즉시 메모리를 돌려준다
            val savedPath = try {
                saveEditedImageUseCase(edited.toAndroidBitmap()).getOrNull()
            } finally {
                edited.recycle()
            }
            updateState { copy(isSaving = false) }

            if (savedPath == null) {
                postSideEffect(ToppingEditEffect.SaveFailed)
                return@launch
            }

            postSideEffect(ToppingEditEffect.EditCompleted(savedPath))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sourceImageUri") sourceImageUri: String,
            @Assisted("segmentationImageUri") segmentationImageUri: String,
        ): ToppingEditViewModel
    }
}
