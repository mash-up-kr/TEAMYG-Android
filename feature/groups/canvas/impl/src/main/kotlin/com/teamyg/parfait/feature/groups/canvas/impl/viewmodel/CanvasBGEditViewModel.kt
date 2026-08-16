package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

enum class CanvasEditTab { BACKGROUND, TOPPING }

data class CanvasToppingItem(
    val parfaitImageId: Long,
    val isMine: Boolean,
    val imageResId: Int,
    val offsetX: Dp,
    val offsetY: Dp,
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    /** 테두리 편집을 거쳐 새로 구운 이미지 경로. 있으면 [imageResId] 대신 이걸 그린다. */
    val editedImagePath: String? = null,
)

private const val TOPPING_MIN_SCALE = 0.5f
private const val TOPPING_MAX_SCALE = 2.5f

/** 세로로 이 픽셀만큼 드래그해야 배율이 1.0만큼 바뀐다 */
private const val TOPPING_DRAG_PX_PER_SCALE = 300f

/** 가로로 1픽셀 드래그할 때 회전하는 각도 */
private const val TOPPING_DRAG_DEGREES_PER_PX = 0.5f

val CanvasBackgroundPaletteColors = listOf(
    YGAtomicColors.Gray.White,
    YGAtomicColors.Gray.Black,
    YGAtomicColors.Cherry.Cherry200,
    Color(0xFFFCE7C2),
    Color(0xFFF9F9AB),
    Color(0xFFC5FFD7),
    Color(0xFFC2E4FC),
    Color(0xFFDCC2FC),
)

data class CanvasBGEditUiState(
    val selectedTab: CanvasEditTab = CanvasEditTab.BACKGROUND,
    // TODO: 저장된 배경이 있으면 그 배경을, 없으면 첫 팔레트 색상을 기본값으로 불러와야 함
    val selectedColor: Color = CanvasBackgroundPaletteColors.first(),
    val selectedImageUri: String? = null,
    val selectedImageSource: PictureConfirmSource? = null,
    val showQuitDialog: Boolean = false,
    val toppings: List<CanvasToppingItem> = emptyList(),
    val selectedToppingId: Long? = null,
    val showDeleteToppingDialog: Boolean = false,
) : UiState

sealed interface CanvasBGEditIntent : UiIntent {
    data class OnSelectTab(
        val tab: CanvasEditTab,
    ) : CanvasBGEditIntent

    data class OnSelectColor(
        val color: Color,
    ) : CanvasBGEditIntent

    data class OnBackgroundImageResult(
        val uri: String,
        val source: PictureConfirmSource,
    ) : CanvasBGEditIntent

    class OnClickCamera : CanvasBGEditIntent

    class OnClickGallery : CanvasBGEditIntent

    class OnClickCloseButton : CanvasBGEditIntent

    class OnQuitDialogConfirm : CanvasBGEditIntent

    class OnQuitDialogCancel : CanvasBGEditIntent

    class OnClickConfirm : CanvasBGEditIntent

    /** 내 토핑을 탭해 선택하거나, 이미 선택된 토핑을 다시 탭해 선택을 해제한다. */
    data class OnClickTopping(
        val topping: CanvasToppingItem,
    ) : CanvasBGEditIntent

    /** 딤 처리된 영역(배경, 남의 토핑)을 탭해 지금 선택을 해제한다. */
    class OnClickDeselectTopping : CanvasBGEditIntent

    /** 툴바의 삭제 버튼. 바로 지우지 않고 확인 모달을 띄운다. */
    class OnClickDeleteToppingButton : CanvasBGEditIntent

    class OnDeleteToppingDialogConfirm : CanvasBGEditIntent

    class OnDeleteToppingDialogCancel : CanvasBGEditIntent

    class OnClickEditTopping : CanvasBGEditIntent

    /**
     * 크기조절 아이콘을 잡고 드래그한 만큼 넘어온다. 토핑 바깥쪽(우측 상단 대각선) 방향으로 끌면 커지고
     * 안쪽으로 끌면 작아진다.
     */
    data class OnToppingResizeDrag(
        val delta: Offset,
    ) : CanvasBGEditIntent

    /** 회전 아이콘을 잡고 드래그한 만큼 넘어온다. 가로 드래그 거리만큼 회전한다. */
    data class OnToppingRotateDrag(
        val delta: Offset,
    ) : CanvasBGEditIntent

    /** 선택된 토핑 자신을 잡고 드래그한 만큼 넘어온다. 드래그한 그대로 위치를 옮긴다. */
    data class OnToppingMoveDrag(
        val delta: DpOffset,
    ) : CanvasBGEditIntent

    /** 테두리 편집 화면에서 돌아온 결과. 편집을 시작한 토핑에 새 이미지·테두리를 반영한다. */
    data class OnToppingEditResult(
        val toppingId: Long,
        val result: ToppingEditResult,
    ) : CanvasBGEditIntent
}

sealed interface CanvasBGEditEffect : UiSideEffect {
    class NavigateToCamera : CanvasBGEditEffect

    class NavigateToGallery : CanvasBGEditEffect

    class NavigateBack : CanvasBGEditEffect

    data class ConfirmBackground(
        val background: YGCanvasBackground,
    ) : CanvasBGEditEffect

    data class NavigateToToppingEdit(
        val toppingId: Long,
        val sourceImageUri: String,
        val segmentationImageUri: String,
        val borderLayers: List<ToppingBorderLayer>,
    ) : CanvasBGEditEffect
}

@HiltViewModel
class CanvasBGEditViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
        loadMockToppings()
    }

    private fun loadMockToppings() {
        // TODO: 실제 "캔버스에 놓인 토핑 목록 조회" API 연동 필요 - 지금은 mock, 위치도 하드코딩
        fun mockUri(@Suppress("SameParameterValue") resId: Int) = "android.resource://${context.packageName}/$resId"

        updateState {
            copy(
                toppings = listOf(
                    CanvasToppingItem(
                        parfaitImageId = 1L,
                        isMine = true,
                        imageResId = DesignSystemR.drawable.img_topping_template_01,
                        offsetX = 24.dp,
                        offsetY = 40.dp,
                        sourceImageUri = mockUri(DesignSystemR.drawable.img_topping_template_01),
                        segmentationImageUri = mockUri(DesignSystemR.drawable.img_topping_template_01),
                    ),
                    CanvasToppingItem(
                        parfaitImageId = 2L,
                        isMine = false,
                        imageResId = DesignSystemR.drawable.img_topping_template_02,
                        offsetX = 140.dp,
                        offsetY = 90.dp,
                        sourceImageUri = mockUri(DesignSystemR.drawable.img_topping_template_02),
                        segmentationImageUri = mockUri(DesignSystemR.drawable.img_topping_template_02),
                    ),
                    CanvasToppingItem(
                        parfaitImageId = 3L,
                        isMine = true,
                        imageResId = DesignSystemR.drawable.img_topping_template_03,
                        offsetX = 60.dp,
                        offsetY = 220.dp,
                        sourceImageUri = mockUri(DesignSystemR.drawable.img_topping_template_03),
                        segmentationImageUri = mockUri(DesignSystemR.drawable.img_topping_template_03),
                    ),
                    CanvasToppingItem(
                        parfaitImageId = 4L,
                        isMine = false,
                        imageResId = DesignSystemR.drawable.img_topping_template_04,
                        offsetX = 160.dp,
                        offsetY = 260.dp,
                        sourceImageUri = mockUri(DesignSystemR.drawable.img_topping_template_04),
                        segmentationImageUri = mockUri(DesignSystemR.drawable.img_topping_template_04),
                    ),
                ),
            )
        }
    }

    override fun processIntent(intent: CanvasBGEditIntent) {
        when (intent) {
            is CanvasBGEditIntent.OnSelectTab -> updateState { copy(selectedTab = intent.tab) }
            is CanvasBGEditIntent.OnSelectColor -> handleOnSelectColor(intent)
            is CanvasBGEditIntent.OnBackgroundImageResult -> handleOnBackgroundImageResult(intent)
            is CanvasBGEditIntent.OnClickCamera -> postSideEffect(effect = CanvasBGEditEffect.NavigateToCamera())
            is CanvasBGEditIntent.OnClickGallery -> postSideEffect(effect = CanvasBGEditEffect.NavigateToGallery())
            is CanvasBGEditIntent.OnClickCloseButton -> updateState { copy(showQuitDialog = true) }
            is CanvasBGEditIntent.OnQuitDialogConfirm -> postSideEffect(effect = CanvasBGEditEffect.NavigateBack())
            is CanvasBGEditIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }
            is CanvasBGEditIntent.OnClickConfirm -> handleOnClickConfirm()
            is CanvasBGEditIntent.OnClickTopping -> handleOnClickTopping(intent)
            is CanvasBGEditIntent.OnClickDeselectTopping -> handleOnClickDeselectTopping()
            is CanvasBGEditIntent.OnClickDeleteToppingButton -> updateState { copy(showDeleteToppingDialog = true) }
            is CanvasBGEditIntent.OnDeleteToppingDialogConfirm -> handleOnDeleteToppingDialogConfirm()
            is CanvasBGEditIntent.OnDeleteToppingDialogCancel -> updateState { copy(showDeleteToppingDialog = false) }
            is CanvasBGEditIntent.OnClickEditTopping -> handleOnClickEditTopping()
            is CanvasBGEditIntent.OnToppingResizeDrag -> handleOnToppingResizeDrag(intent)
            is CanvasBGEditIntent.OnToppingRotateDrag -> handleOnToppingRotateDrag(intent)
            is CanvasBGEditIntent.OnToppingMoveDrag -> handleOnToppingMoveDrag(intent)
            is CanvasBGEditIntent.OnToppingEditResult -> handleOnToppingEditResult(intent)
        }
    }

    private fun handleOnClickTopping(intent: CanvasBGEditIntent.OnClickTopping) {
        if (!intent.topping.isMine) {
            return
        }

        val newSelectedId = if (state.value.selectedToppingId == intent.topping.parfaitImageId) {
            null
        } else {
            intent.topping.parfaitImageId
        }

        updateState { copy(selectedToppingId = newSelectedId) }
    }

    private fun handleOnClickDeselectTopping() {
        updateState { copy(selectedToppingId = null) }
    }

    private fun handleOnDeleteToppingDialogConfirm() {
        val selectedId = state.value.selectedToppingId ?: return

        updateState {
            copy(
                toppings = toppings.filterNot { it.parfaitImageId == selectedId },
                selectedToppingId = null,
                showDeleteToppingDialog = false,
            )
        }
    }

    private fun handleOnToppingResizeDrag(intent: CanvasBGEditIntent.OnToppingResizeDrag) {
        val selectedId = state.value.selectedToppingId ?: return
        val current = state.value.toppings.find { it.parfaitImageId == selectedId } ?: return

        // 핸들이 우측 상단 모서리에 있으므로, 그 모서리의 바깥쪽 방향으로 끌면 커지고 안쪽이면 작아진다
        val (outX, outY) = resizeOutwardDirection(current.rotationDegrees)
        val deltaScale = (intent.delta.x * outX + intent.delta.y * outY) / TOPPING_DRAG_PX_PER_SCALE
        val newScale = (current.scale + deltaScale).coerceIn(TOPPING_MIN_SCALE, TOPPING_MAX_SCALE)

        applyToppingTransform(selectedId) { topping ->
            topping.copy(scale = newScale)
        }
    }

    private fun handleOnToppingRotateDrag(intent: CanvasBGEditIntent.OnToppingRotateDrag) {
        val selectedId = state.value.selectedToppingId ?: return

        val deltaDegrees = intent.delta.x * TOPPING_DRAG_DEGREES_PER_PX
        applyToppingTransform(selectedId) { topping ->
            topping.copy(rotationDegrees = topping.rotationDegrees + deltaDegrees)
        }
    }

    private fun handleOnToppingMoveDrag(intent: CanvasBGEditIntent.OnToppingMoveDrag) {
        val selectedId = state.value.selectedToppingId ?: return

        applyToppingTransform(selectedId) { topping ->
            topping.copy(
                offsetX = topping.offsetX + intent.delta.x,
                offsetY = topping.offsetY + intent.delta.y,
            )
        }
    }

    /**
     * [transform]으로 크기/회전/위치를 바꾼다. 캔버스 밖으로 나가는 부분은 화면에서 클립되어 안 보이므로
     * 여기서 위치를 되돌리지 않고 [transform] 결과를 그대로 반영한다.
     */
    private fun applyToppingTransform(
        toppingId: Long,
        transform: (CanvasToppingItem) -> CanvasToppingItem,
    ) {
        updateState {
            copy(
                toppings = toppings.map { topping ->
                    if (topping.parfaitImageId != toppingId) topping else transform(topping)
                },
            )
        }
    }

    private fun handleOnClickEditTopping() {
        val selected = state.value.toppings.find { it.parfaitImageId == state.value.selectedToppingId } ?: return

        postSideEffect(
            effect = CanvasBGEditEffect.NavigateToToppingEdit(
                toppingId = selected.parfaitImageId,
                sourceImageUri = selected.sourceImageUri,
                segmentationImageUri = selected.segmentationImageUri,
                borderLayers = selected.borderLayers,
            ),
        )
    }

    private fun handleOnToppingEditResult(intent: CanvasBGEditIntent.OnToppingEditResult) {
        applyToppingTransform(intent.toppingId) { topping ->
            topping.copy(
                // 다시 편집을 열 때 이 사진에서 시작해야 방금 지운/되살린 영역이 유지된다
                segmentationImageUri = File(intent.result.cutoutImagePath).toUri().toString(),
                borderLayers = intent.result.borderLayers,
                editedImagePath = intent.result.editedImagePath,
            )
        }
    }

    private fun handleOnSelectColor(intent: CanvasBGEditIntent.OnSelectColor) {
        updateState {
            copy(
                selectedColor = intent.color,
                selectedImageUri = null,
                selectedImageSource = null,
            )
        }
    }

    private fun handleOnBackgroundImageResult(intent: CanvasBGEditIntent.OnBackgroundImageResult) {
        updateState {
            copy(
                selectedImageUri = intent.uri,
                selectedImageSource = intent.source,
            )
        }
    }

    private fun handleOnClickConfirm() {
        val imageUri = state.value.selectedImageUri
        val background = if (imageUri != null) {
            YGCanvasBackground.Image(url = imageUri)
        } else {
            YGCanvasBackground.Solid(state.value.selectedColor)
        }

        postSideEffect(effect = CanvasBGEditEffect.ConfirmBackground(background))
    }
}
