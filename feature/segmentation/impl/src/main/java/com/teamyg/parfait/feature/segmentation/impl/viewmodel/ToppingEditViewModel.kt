package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveEditedImageUseCase
import com.teamyg.parfait.feature.segmentation.impl.editor.DEFAULT_TOPPING_BORDER_COLOR
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingBorderStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditHistory
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

/**
 * 테두리 굵기의 mock 범위. 영역 붓과 달리 결과 이미지 바깥에 두르는 선이라 원본 해상도에 매이지 않는다.
 */
// Todo : 실제 테두리 스펙이 정해지면 값 교체
private const val DEFAULT_BORDER_WIDTH = 4f
private const val MIN_BORDER_WIDTH = 1f
private const val MAX_BORDER_WIDTH = 20f

data class ToppingEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val tab: ToppingEditTab = ToppingEditTab.AREA,
    val mode: ToppingEditMode = ToppingEditMode.ERASE,
    val brushWidthDp: Float = DEFAULT_BRUSH_WIDTH_DP,
    val areaHistory: ToppingEditHistory<ToppingEditStroke> = ToppingEditHistory(),
    val borderWidth: Float = DEFAULT_BORDER_WIDTH,
    val borderHistory: ToppingEditHistory<ToppingBorderStroke> = ToppingEditHistory(),
    val isSaving: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    /** 영역 탭에서 확정된 획. 캔버스와 저장이 함께 본다 */
    val strokes: List<ToppingEditStroke> get() = areaHistory.done

    /** 안쪽부터 바깥쪽 순으로 중첩된 테두리 겹 */
    val borderStrokes: List<ToppingBorderStroke> get() = borderHistory.done

    /**
     * 색상칩에서 켜둘 색. 가장 바깥 겹의 색이다.
     * 되돌리면 그 아래 겹의 색으로 돌아가고, 겹이 다 벗겨지면 처음처럼 투명 칩이 켜진다.
     */
    val selectedBorderColor: Color get() = borderStrokes.lastOrNull()?.color ?: DEFAULT_TOPPING_BORDER_COLOR

    val minBrushWidthDp: Float get() = MIN_BRUSH_WIDTH_DP

    val maxBrushWidthDp: Float get() = MAX_BRUSH_WIDTH_DP

    val minBorderWidth: Float get() = MIN_BORDER_WIDTH

    val maxBorderWidth: Float get() = MAX_BORDER_WIDTH

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

/**
 * 되돌리기/다시 실행은 탭마다 스택이 따로여서 intent 도 탭별로 나눈다.
 * 현재 탭을 보고 한쪽으로 흘려보내면 탭이 바뀌는 순간 어느 스택을 건드리는지가 흐려진다.
 */
sealed interface ToppingEditIntent : UiIntent {
    data class ChangeTab(val tab: ToppingEditTab) : ToppingEditIntent

    data class ChangeMode(val mode: ToppingEditMode) : ToppingEditIntent

    data class ChangeBrushWidth(val width: Float) : ToppingEditIntent

    /** 드래그가 끝난 획을 확정한다. 그리는 도중의 획은 화면이 들고 있는다 */
    data class AddStroke(val stroke: ToppingEditStroke) : ToppingEditIntent

    data object UndoArea : ToppingEditIntent

    data object RedoArea : ToppingEditIntent

    /** 고른 색으로 현재 굵기의 테두리를 한 겹 더 두른다 */
    data class AddBorderStroke(val color: Color) : ToppingEditIntent

    data class ChangeBorderWidth(val width: Float) : ToppingEditIntent

    data object UndoBorder : ToppingEditIntent

    data object RedoBorder : ToppingEditIntent

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
                updateState { copy(areaHistory = areaHistory.push(intent.stroke)) }
            }

            ToppingEditIntent.UndoArea -> {
                updateState { copy(areaHistory = areaHistory.undo()) }
            }

            ToppingEditIntent.RedoArea -> {
                updateState { copy(areaHistory = areaHistory.redo()) }
            }

            is ToppingEditIntent.AddBorderStroke -> {
                updateState {
                    val stroke = ToppingBorderStroke(color = intent.color, width = borderWidth)
                    copy(borderHistory = borderHistory.push(stroke))
                }
            }

            is ToppingEditIntent.ChangeBorderWidth -> {
                // 굵기는 다음에 두를 겹에만 적용된다. 이미 쌓인 겹은 그대로 둔다
                updateState { copy(borderWidth = intent.width.coerceIn(minBorderWidth, maxBorderWidth)) }
            }

            ToppingEditIntent.UndoBorder -> {
                updateState { copy(borderHistory = borderHistory.undo()) }
            }

            ToppingEditIntent.RedoBorder -> {
                updateState { copy(borderHistory = borderHistory.redo()) }
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
