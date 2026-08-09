package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveEditedImageUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import com.teamyg.parfait.feature.segmentation.impl.editor.DEFAULT_TOPPING_BORDER_COLOR
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMode
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditTab
import com.teamyg.parfait.feature.segmentation.impl.editor.UndoRedoStack
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.color
import com.teamyg.parfait.feature.segmentation.impl.editor.withBorders
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
 * 테두리 굵기(px). 기획 정책(C-104)이 해상도와 무관한 고정 px 범위로 못박아 둔 값이다.
 *
 * 굵기는 원본 좌표로 재고 저장 결과도 원본 크기라, 여기 적힌 px 가 곧 저장된 파일에서의 px 다.
 * 화면에서는 원본이 편집 영역에 맞춰 줄어든 배율만큼 함께 줄어 보인다.
 */
private const val DEFAULT_BORDER_WIDTH = 10f
private const val MIN_BORDER_WIDTH = 2f
private const val MAX_BORDER_WIDTH = 50f

private fun UndoRedoStack<ToppingBorderLayer>.outermostColor(): Color = latest?.color ?: DEFAULT_TOPPING_BORDER_COLOR

data class ToppingEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val tab: ToppingEditTab = ToppingEditTab.AREA,
    val mode: ToppingEditMode = ToppingEditMode.ERASE,
    val brushWidthDp: Float = DEFAULT_BRUSH_WIDTH_DP,
    /**
     * 되돌리기 스택은 탭마다 따로다.
     *
     * 영역 탭에서 획을 지운 뒤 테두리 탭에서 되돌리기를 눌러도 획이 살아나면 안 되기 때문이다.
     */
    val areaHistory: UndoRedoStack<ToppingEditStroke> = UndoRedoStack(),
    /** 아직 두르지 않은, 다음 겹에 쓸 굵기. 두른 겹이 있으면 [borderWidth] 가 그 겹을 따라간다 */
    val pendingBorderWidth: Float = DEFAULT_BORDER_WIDTH,
    val borderHistory: UndoRedoStack<ToppingBorderLayer> = UndoRedoStack(),
    /**
     * 색상칩에서 켜둘 색.
     *
     * 보통은 가장 바깥 겹의 색이지만, 두를 색이 없는 투명 칩도 고른 표시는 남아야 해서
     * 겹에서 끌어내지 않고 따로 들고 간다.
     */
    val selectedBorderColor: Color = DEFAULT_TOPPING_BORDER_COLOR,
    val isSaving: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    /** 영역 탭에서 확정된 획. 캔버스와 저장이 함께 본다 */
    val strokes: List<ToppingEditStroke> get() = areaHistory.done

    val borderLayers: List<ToppingBorderLayer> get() = borderHistory.done

    val minBrushWidthDp: Float get() = MIN_BRUSH_WIDTH_DP

    val maxBrushWidthDp: Float get() = MAX_BRUSH_WIDTH_DP

    /**
     * 굵기 슬라이더가 가리키는 값. 두른 겹이 있으면 슬라이더가 곧 그 겹의 굵기다.
     * 되돌려서 겹이 벗겨지면 그 아래 겹의 굵기로 따라 내려간다.
     */
    val borderWidth: Float get() = borderHistory.latest?.width ?: pendingBorderWidth

    val minBorderWidth: Float get() = MIN_BORDER_WIDTH

    val maxBorderWidth: Float get() = MAX_BORDER_WIDTH
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

    /** 고른 색으로 현재 굵기의 테두리를 한 겹 더 두른다. 투명 칩은 두를 색이 없어 아무것도 하지 않는다 */
    data class SelectBorderColor(val color: Color) : ToppingEditIntent

    data class ChangeBorderWidth(val width: Float) : ToppingEditIntent

    data object UndoBorder : ToppingEditIntent

    data object RedoBorder : ToppingEditIntent

    data object ClickDone : ToppingEditIntent
}

sealed interface ToppingEditEffect : UiSideEffect {
    data object LoadFailed : ToppingEditEffect

    data object SaveFailed : ToppingEditEffect

    data class EditCompleted(val result: ToppingEditResult) : ToppingEditEffect
}

@HiltViewModel(assistedFactory = ToppingEditViewModel.Factory::class)
class ToppingEditViewModel
@AssistedInject constructor(
    @Assisted("sourceImageUri") private val sourceImageUri: String,
    @Assisted("segmentationImageUri") private val segmentationImageUri: String,
    @Assisted("borderLayers") private val initialBorderLayers: List<ToppingBorderLayer>,
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

            is ToppingEditIntent.SelectBorderColor -> {
                updateState {
                    // 겹을 더하지 않는 색이라고 두른 겹을 걷어내지는 않는다. 걷어내는 건 되돌리기가 할 일이다
                    val addsNothing = intent.color == DEFAULT_TOPPING_BORDER_COLOR ||
                        intent.color == borderHistory.outermostColor()
                    if (addsNothing) return@updateState copy(selectedBorderColor = intent.color)

                    val layer = ToppingBorderLayer(colorArgb = intent.color.toArgb(), width = borderWidth)
                    copy(
                        selectedBorderColor = intent.color,
                        borderHistory = borderHistory.push(layer),
                    )
                }
            }

            is ToppingEditIntent.ChangeBorderWidth -> {
                updateState {
                    val width = intent.width.coerceIn(minBorderWidth, maxBorderWidth)
                    // 슬라이더는 가장 바깥 겹의 굵기를 직접 민다. 두른 겹이 없으면 다음 겹에 쓸 값만 바뀐다
                    copy(
                        pendingBorderWidth = width,
                        borderHistory = borderHistory.replaceLast { layer -> layer.copy(width = width) },
                    )
                }
            }

            ToppingEditIntent.UndoBorder -> {
                updateState {
                    val history = borderHistory.undo()
                    copy(borderHistory = history, selectedBorderColor = history.outermostColor())
                }
            }

            ToppingEditIntent.RedoBorder -> {
                updateState {
                    val history = borderHistory.redo()
                    copy(borderHistory = history, selectedBorderColor = history.outermostColor())
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

            // 이미 두른 테두리를 겹째로 물려받아, 다시 편집해도 벗겨진 채로 열리지 않는다
            val restoredBorders = UndoRedoStack(done = initialBorderLayers)

            updateState {
                copy(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    borderHistory = restoredBorders,
                    selectedBorderColor = restoredBorders.outermostColor(),
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

            // 테두리를 구운 결과에서는 알맹이를 되짚을 수 없어, 두르기 전 알맹이도 함께 남긴다
            val cutout = withContext(Dispatchers.Default) {
                buildCutoutBitmap(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    strokes = current.strokes,
                )
            }
            val edited = withContext(Dispatchers.Default) { cutout.withBorders(current.borderLayers) }

            // 화면 사이에서는 비트맵 대신 경로를 주고받으므로 여기서 파일로 떨군다.
            // 저장 전용으로 만든 비트맵이라 화면이 잡고 있지 않고, 원본 해상도라 수십 MB 에
            // 이르기도 해서 파일로 떨구는 즉시 메모리를 돌려준다
            val (cutoutPath, editedPath) = try {
                val savedCutoutPath = saveEditedImageUseCase(cutout.toAndroidBitmap()).getOrNull()
                // 테두리가 없으면 알맹이가 곧 결과라 같은 파일을 두 번 떨구지 않는다
                val savedEditedPath = if (current.borderLayers.isEmpty()) {
                    savedCutoutPath
                } else {
                    saveEditedImageUseCase(edited.toAndroidBitmap()).getOrNull()
                }
                savedCutoutPath to savedEditedPath
            } finally {
                // 테두리가 없으면 두르기 전후가 같은 비트맵이라 한 번만 돌려준다
                if (edited !== cutout) edited.recycle()
                cutout.recycle()
            }
            updateState { copy(isSaving = false) }

            if (cutoutPath == null || editedPath == null) {
                postSideEffect(ToppingEditEffect.SaveFailed)
                return@launch
            }

            postSideEffect(
                ToppingEditEffect.EditCompleted(
                    ToppingEditResult(
                        editedImagePath = editedPath,
                        cutoutImagePath = cutoutPath,
                        borderLayers = current.borderLayers,
                    ),
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("sourceImageUri") sourceImageUri: String,
            @Assisted("segmentationImageUri") segmentationImageUri: String,
            @Assisted("borderLayers") borderLayers: List<ToppingBorderLayer>,
        ): ToppingEditViewModel
    }
}
