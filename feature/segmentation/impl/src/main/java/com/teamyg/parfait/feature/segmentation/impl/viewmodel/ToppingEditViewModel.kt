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
 * 화면에 보이는 테두리 굵기. 기획 정책(C-104)이 정한 2~50 범위를 붓과 같은 dp 기준으로 잡는다.
 *
 * 원본 좌표로 재면 사진 해상도에 따라 같은 값이 전혀 다른 굵기로 보이므로 붓과 단위를 맞춘다.
 * 저장할 때 편집 화면이 미리보기에 쓴 배율을 되짚어 원본 비트맵 좌표계 굵기로 환산한다.
 */
private const val DEFAULT_BORDER_WIDTH_DP = 10f
private const val MIN_BORDER_WIDTH_DP = 2f
private const val MAX_BORDER_WIDTH_DP = 50f

data class ToppingEditState(
    val originBitmap: Bitmap? = null,
    val segmentationBitmap: Bitmap? = null,
    val tab: ToppingEditTab = ToppingEditTab.AREA,
    /** 이미 캔버스에 놓인 토핑을 다시 손보는 중이면 영역은 못 건드리고 테두리만 고칠 수 있다 */
    val isBorderOnly: Boolean = false,
    val mode: ToppingEditMode = ToppingEditMode.ERASE,
    val brushWidthDp: Float = DEFAULT_BRUSH_WIDTH_DP,
    /**
     * 되돌리기 스택은 탭마다 따로다.
     *
     * 영역 탭에서 획을 지운 뒤 테두리 탭에서 되돌리기를 눌러도 획이 살아나면 안 되기 때문이다.
     */
    val areaHistory: UndoRedoStack<ToppingEditStroke> = UndoRedoStack(),
    /** 아직 아무 색도 고르지 않았을 때 쓸 굵기. 고른 테두리가 있으면 [borderWidthDp] 가 그 테두리를 따라간다 */
    val pendingBorderWidthDp: Float = DEFAULT_BORDER_WIDTH_DP,
    /**
     * 테두리를 고른 이력. 뒤쪽이 지금 둘러진 테두리다.
     *
     * 테두리는 겹쳐 두를 수 없어 언제나 마지막에 고른 하나만 둘러지므로,
     * 이 스택은 두른 겹이 아니라 무엇을 골랐는지를 순서대로 쌓는다.
     * 빨강 → 흰색 → 초록 순으로 골랐다면 되돌리기는 초록 → 흰색 → 빨강 순으로 물러난다.
     */
    val borderHistory: UndoRedoStack<ToppingBorderLayer> = UndoRedoStack(),
    /**
     * dp 굵기를 원본 비트맵 좌표계 굵기로 바꿀 때 곱할 값.
     *
     * 미리보기가 사진을 편집 영역에 맞춰 줄여 놓은 배율을 되짚은 값이라 화면을 재 본 뒤에야 알 수 있다.
     * 편집 영역을 그리는 쪽이 크기를 재서 알려준다.
     */
    val originPxPerDp: Float = 0f,
    val isSaving: Boolean = false,
) : UiState {
    val isLoading: Boolean get() = originBitmap == null || segmentationBitmap == null

    /** 영역 탭에서 확정된 획. 캔버스와 저장이 함께 본다 */
    val strokes: List<ToppingEditStroke> get() = areaHistory.done

    /**
     * 지금 둘러진 테두리. 겹칠 수 없으니 마지막에 고른 하나뿐이다.
     *
     * 투명 칩은 색이 아니라 두르지 않음을 가리키므로, 골랐다는 이력만 남고 두를 테두리는 없다.
     */
    val borderLayers: List<ToppingBorderLayer>
        get() = listOfNotNull(borderHistory.latest?.takeIf { layer -> layer.color != DEFAULT_TOPPING_BORDER_COLOR })

    /** 색상칩에서 켜둘 색. 마지막에 고른 것이 곧 켜둘 색이라, 아무것도 고르지 않았으면 투명 칩이 켜진다 */
    val selectedBorderColor: Color get() = borderHistory.latest?.color ?: DEFAULT_TOPPING_BORDER_COLOR

    val minBrushWidthDp: Float get() = MIN_BRUSH_WIDTH_DP

    val maxBrushWidthDp: Float get() = MAX_BRUSH_WIDTH_DP

    /**
     * 굵기 슬라이더가 가리키는 값. 고른 테두리가 있으면 슬라이더가 곧 그 테두리의 굵기다.
     * 되돌려서 이전 선택으로 물러나면 그 선택의 굵기로 따라간다.
     */
    val borderWidthDp: Float get() = borderHistory.latest?.widthDp ?: pendingBorderWidthDp

    val minBorderWidthDp: Float get() = MIN_BORDER_WIDTH_DP

    val maxBorderWidthDp: Float get() = MAX_BORDER_WIDTH_DP
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

    /**
     * 고른 색으로 테두리를 갈아 두른다. 겹칠 수 없어 직전 테두리는 남지 않는다.
     * 투명 칩은 두를 색이 없어 테두리를 벗기며, 어느 쪽이든 고른 이력은 남아 되돌릴 수 있다.
     */
    data class SelectBorderColor(val color: Color) : ToppingEditIntent

    data class ChangeBorderWidth(val width: Float) : ToppingEditIntent

    /** 편집 영역을 다 재고 나서 dp 를 원본 좌표로 되돌릴 값을 알려준다 */
    data class ChangeOriginPxPerDp(val originPxPerDp: Float) : ToppingEditIntent

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
    @Assisted("borderOnly") borderOnly: Boolean,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val saveEditedImageUseCase: SaveEditedImageUseCase,
) : BaseViewModel<ToppingEditState, ToppingEditIntent, ToppingEditEffect>(
    initialState = ToppingEditState(
        tab = if (borderOnly) ToppingEditTab.BORDER else ToppingEditTab.AREA,
        isBorderOnly = borderOnly,
    ),
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
                    // 켜져 있는 칩을 다시 눌러도 달라지는 게 없어 되돌릴 칸을 쌓지 않는다
                    if (intent.color == selectedBorderColor) return@updateState this

                    val selection = ToppingBorderLayer(colorArgb = intent.color.toArgb(), widthDp = borderWidthDp)
                    copy(borderHistory = borderHistory.push(selection))
                }
            }

            is ToppingEditIntent.ChangeBorderWidth -> {
                updateState {
                    val widthDp = intent.width.coerceIn(minBorderWidthDp, maxBorderWidthDp)
                    // 슬라이더는 지금 고른 테두리의 굵기를 직접 민다. 고른 게 없으면 다음에 쓸 값만 바뀐다
                    copy(
                        pendingBorderWidthDp = widthDp,
                        borderHistory = borderHistory.replaceLast { layer -> layer.copy(widthDp = widthDp) },
                    )
                }
            }

            is ToppingEditIntent.ChangeOriginPxPerDp -> {
                updateState { copy(originPxPerDp = intent.originPxPerDp) }
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

            // 이미 두른 테두리를 고른 이력으로 물려받아, 다시 편집해도 벗겨진 채로 열리지 않는다
            updateState {
                copy(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    borderHistory = UndoRedoStack(done = initialBorderLayers),
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
            val edited = withContext(Dispatchers.Default) {
                cutout.withBorders(current.borderLayers, current.originPxPerDp)
            }

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
            @Assisted("borderOnly") borderOnly: Boolean,
        ): ToppingEditViewModel
    }
}
