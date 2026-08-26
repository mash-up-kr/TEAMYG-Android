package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.core.util.android.extension.toRgbHex
import com.teamyg.parfait.core.util.android.extension.toRgbHexString
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.usecase.image.UploadImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ChangeCanvasBackgroundUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File

enum class CanvasEditTab { BACKGROUND, TOPPING }

/**
 * 편집 화면이 다루는 토핑 하나.
 *
 * 위치·크기가 Dp 가 아니라 Canvas-Area 대비 0~1 비율인 이유: 저장된 배치가 그 단위이고
 * (`CanvasToppingVO.transform`), ViewModel 은 화면 크기를 모른다. Dp 로 들고 있으면 기기마다
 * 다른 자리에 놓이고 캔버스 메인([CanvasToppingLayer])과도 어긋난다 — 같은 캔버스가 두
 * 화면에서 다르게 보이면 안 된다.
 *
 * @param imageUrl 서버에 저장된 토핑 이미지 주소.
 * @param editedImagePath 테두리 편집을 거쳐 새로 구운 이미지의 로컬 경로. 있으면 [imageUrl]
 *   대신 이걸 그린다 — 아직 서버에 올리기 전이라 이쪽이 최신이다.
 * @param cutoutImagePath 테두리를 두르기 전 알맹이. 다시 편집할 때의 시작 마스크라
 *   [editedImagePath] 로 대신할 수 없다 — 그쪽엔 테두리가 이미 구워져 있다.
 */
data class CanvasToppingItem(
    val parfaitImageId: Long,
    val isMine: Boolean,
    val imageUrl: String,
    val positionX: Float,
    val positionY: Float,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val editedImagePath: String? = null,
    val cutoutImagePath: String? = null,
)

private const val TOPPING_MIN_SCALE = 0.5f

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

/**
 * @param selectedImageUri 배경으로 고른 이미지. 방금 기기에서 고른 사진일 수도, 이미 서버에
 *   저장돼 있던 배경의 URL 일 수도 있다. 둘을 가르는 것은 [selectedImageSource] 다.
 * @param selectedImageSource 이미지를 어디서 가져왔는지. **null 이면 서버에 이미 있는 배경**이라
 *   확인을 눌러도 다시 올리지 않는다 — https 주소는 기기에서 읽을 수 없어 올릴 수도 없다.
 */
data class CanvasBGEditUiState(
    val selectedTab: CanvasEditTab = CanvasEditTab.BACKGROUND,
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

    data object OnClickCamera : CanvasBGEditIntent

    data object OnClickGallery : CanvasBGEditIntent

    data object OnClickCloseButton : CanvasBGEditIntent

    data object OnQuitDialogConfirm : CanvasBGEditIntent

    data object OnQuitDialogCancel : CanvasBGEditIntent

    data object OnClickConfirm : CanvasBGEditIntent

    /** 내 토핑을 탭해 선택하거나, 이미 선택된 토핑을 다시 탭해 선택을 해제한다. */
    data class OnClickTopping(
        val topping: CanvasToppingItem,
    ) : CanvasBGEditIntent

    /** 딤 처리된 영역(배경, 남의 토핑)을 탭해 지금 선택을 해제한다. */
    data object OnClickDeselectTopping : CanvasBGEditIntent

    /** 툴바의 삭제 버튼. 바로 지우지 않고 확인 모달을 띄운다. */
    data object OnClickDeleteToppingButton : CanvasBGEditIntent

    data object OnDeleteToppingDialogConfirm : CanvasBGEditIntent

    data object OnDeleteToppingDialogCancel : CanvasBGEditIntent

    data object OnClickEditTopping : CanvasBGEditIntent

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

    /**
     * 선택된 토핑 자신을 잡고 드래그한 만큼 넘어온다.
     *
     * 픽셀이 아니라 **Canvas-Area 대비 비율**이다 — 위치를 그 단위로 들고 있으므로
     * ([CanvasToppingItem]) 화면 크기를 아는 쪽에서 미리 환산해 넘긴다.
     */
    data class OnToppingMoveDrag(
        val deltaX: Float,
        val deltaY: Float,
    ) : CanvasBGEditIntent

    /** 테두리 편집 화면에서 돌아온 결과. 편집을 시작한 토핑에 새 이미지·테두리를 반영한다. */
    data class OnToppingEditResult(
        val toppingId: Long,
        val result: ToppingEditResult,
    ) : CanvasBGEditIntent
}

sealed interface CanvasBGEditEffect : UiSideEffect {
    data object NavigateToCamera : CanvasBGEditEffect

    data object NavigateToGallery : CanvasBGEditEffect

    data object NavigateBack : CanvasBGEditEffect

    /** 배경이 **서버에 저장된 뒤에만** 나간다. 이 이펙트를 받은 화면은 저장된 것으로 여겨도 된다 */
    data class ConfirmBackground(
        val background: YGCanvasBackground,
    ) : CanvasBGEditEffect

    data class ShowError(
        val error: CanvasBGEditError,
    ) : CanvasBGEditEffect

    data class NavigateToToppingEdit(
        val toppingId: Long,
        val sourceImageUri: String,
        val segmentationImageUri: String,
        val borderLayers: List<ToppingBorderLayer>,
    ) : CanvasBGEditEffect
}

@HiltViewModel(assistedFactory = CanvasBGEditViewModel.Factory::class)
class CanvasBGEditViewModel
@AssistedInject
constructor(
    @Assisted("groupId") groupIdValue: Long,
    @Assisted("parfaitId") parfaitIdValue: Long,
    private val getTodayParfaitUseCase: GetTodayParfaitUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val changeCanvasBackgroundUseCase: ChangeCanvasBackgroundUseCase,
    private val deleteToppingUseCase: DeleteToppingUseCase,
    private val updateToppingUseCase: UpdateToppingUseCase,
    private val updateToppingBorderUseCase: UpdateToppingBorderUseCase,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    /**
     * 배경을 저장할 대상. 캔버스 메인이 열어 준 오늘의 캔버스로 시작하지만, 편집 중 자정을
     * 넘겨 조회가 **새 날의 캔버스**를 주면 그쪽으로 옮긴다 — 화면에 그려진 토핑과 저장
     * 대상이 갈라지는 편이 더 나쁘다.
     */
    private var parfaitId = ParfaitId(parfaitIdValue)

    /**
     * 서버에서 막 받아온 그대로의 스냅샷. 확인 버튼을 눌렀을 때 [CanvasBGEditUiState.toppings]와
     * 대조해 실제로 바뀐 토핑만 골라 PATCH 하는 데 쓴다 — 화면 렌더링에는 쓰지 않는다.
     */
    private var confirmedToppings: List<CanvasToppingItem> = emptyList()

    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
        loadCanvas()
    }

    /**
     * ⚠️ 오늘 조회는 캔버스가 없으면 만들어 저장한다. 여기서 불러도 캔버스 메인이 이미 만든
     * 것을 다시 받을 뿐이라 늘어나지 않는다 — 이 화면은 그 메인을 거쳐야만 열린다.
     *
     * 조회를 또 하는 이유는 편집을 여는 사이 다른 멤버가 올린 토핑까지 그려야 해서다.
     */
    private fun loadCanvas() {
        launch(key = LOAD_CANVAS_KEY) {
            getTodayParfaitUseCase(groupId)
                .onSuccess { canvas ->
                    if (canvas.parfaitId != parfaitId) {
                        viewModelLogger.e {
                            "편집을 연 캔버스와 조회 결과가 다르다 — 조회 쪽으로 옮긴다" +
                                " (열린 것: ${parfaitId.value}, 받은 것: ${canvas.parfaitId.value})"
                        }
                        parfaitId = canvas.parfaitId
                    }

                    confirmedToppings = canvas.toppings
                        .sortedBy { topping -> topping.transform.positionZ }
                        .map { topping -> topping.toToppingItem() }

                    updateState { withCanvas(canvas = canvas, toppings = confirmedToppings) }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "캔버스를 불러오지 못했다 - parfaitId: ${parfaitId.value}" }
                    postSideEffect(effect = CanvasBGEditEffect.ShowError(throwable.toCanvasBGEditError()))
                }
        }
    }

    /**
     * 저장된 배경을 팔레트의 시작점으로 삼는다. 색을 못 읽으면 기본 색으로 두는데, 그때 확인을
     * 누르면 배경이 팔레트 첫 색으로 바뀐다 — 못 읽는 색을 그대로 되돌려 보내는 것보다 낫다.
     */
    private fun CanvasBGEditUiState.withCanvas(
        canvas: CanvasVO,
        toppings: List<CanvasToppingItem>,
    ): CanvasBGEditUiState = copy(
        toppings = toppings,
        selectedColor = (canvas.background as? CanvasBackground.Color)
            ?.value
            ?.toColorOrNull()
            ?: selectedColor,
        selectedImageUri = (canvas.background as? CanvasBackground.Image)?.url,
        selectedImageSource = null,
    )

    /** 이 토핑을 내가 놓았는지는 서버가 `placedBy.isMine` 으로 직접 알려준다. */
    private fun CanvasToppingVO.toToppingItem(): CanvasToppingItem = CanvasToppingItem(
        parfaitImageId = parfaitImageId.value,
        isMine = placedBy.isMine,
        imageUrl = imageUrl,
        positionX = transform.positionX.toFloat(),
        positionY = transform.positionY.toFloat(),
        scale = transform.scale.toFloat(),
        rotationDegrees = transform.rotation.toFloat(),
        borderLayers = border.toBorderLayers(),
    )

    /**
     * 서버는 테두리를 한 겹으로만 들고 있고 편집 화면은 겹의 목록으로 다룬다 — 한 겹짜리
     * 목록으로 편다. 색을 못 읽으면 겹을 만들지 않는다(임의의 색을 두르는 것보다 낫다).
     */
    private fun ToppingBorder.toBorderLayers(): List<ToppingBorderLayer> = when (this) {
        is ToppingBorder.None -> emptyList()

        is ToppingBorder.Solid ->
            color
                .toColorOrNull()
                ?.let { borderColor ->
                    listOf(ToppingBorderLayer(colorArgb = borderColor.toArgb(), widthDp = width.toFloat()))
                }.orEmpty()
    }

    override fun processIntent(intent: CanvasBGEditIntent) {
        when (intent) {
            is CanvasBGEditIntent.OnSelectTab -> updateState { copy(selectedTab = intent.tab) }
            is CanvasBGEditIntent.OnSelectColor -> handleOnSelectColor(intent)
            is CanvasBGEditIntent.OnBackgroundImageResult -> handleOnBackgroundImageResult(intent)
            CanvasBGEditIntent.OnClickCamera -> postSideEffect(effect = CanvasBGEditEffect.NavigateToCamera)
            CanvasBGEditIntent.OnClickGallery -> postSideEffect(effect = CanvasBGEditEffect.NavigateToGallery)
            CanvasBGEditIntent.OnClickCloseButton -> updateState { copy(showQuitDialog = true) }
            CanvasBGEditIntent.OnQuitDialogConfirm -> postSideEffect(effect = CanvasBGEditEffect.NavigateBack)
            CanvasBGEditIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }
            CanvasBGEditIntent.OnClickConfirm -> handleOnClickConfirm()
            is CanvasBGEditIntent.OnClickTopping -> handleOnClickTopping(intent)
            CanvasBGEditIntent.OnClickDeselectTopping -> handleOnClickDeselectTopping()
            CanvasBGEditIntent.OnClickDeleteToppingButton -> updateState { copy(showDeleteToppingDialog = true) }
            CanvasBGEditIntent.OnDeleteToppingDialogConfirm -> handleOnDeleteToppingDialogConfirm()
            CanvasBGEditIntent.OnDeleteToppingDialogCancel -> updateState { copy(showDeleteToppingDialog = false) }
            CanvasBGEditIntent.OnClickEditTopping -> handleOnClickEditTopping()
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

        updateState { copy(showDeleteToppingDialog = false) }

        launch(key = DELETE_TOPPING_KEY) {
            deleteToppingUseCase(groupId, parfaitId, ParfaitImageId(selectedId))
                .onSuccess {
                    updateState {
                        copy(
                            toppings = toppings.filterNot { it.parfaitImageId == selectedId },
                            selectedToppingId = null,
                        )
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "토핑을 지우지 못했다 - parfaitImageId: $selectedId" }
                }
        }
    }

    private fun handleOnToppingResizeDrag(intent: CanvasBGEditIntent.OnToppingResizeDrag) {
        val selectedId = state.value.selectedToppingId ?: return
        val current = state.value.toppings.find { it.parfaitImageId == selectedId } ?: return

        // 핸들이 우측 상단 모서리에 있으므로, 그 모서리의 바깥쪽 방향으로 끌면 커지고 안쪽이면 작아진다
        val (outX, outY) = resizeOutwardDirection(current.rotationDegrees)
        val deltaScale = (intent.delta.x * outX + intent.delta.y * outY) / TOPPING_DRAG_PX_PER_SCALE
        val newScale = (current.scale + deltaScale).coerceAtLeast(TOPPING_MIN_SCALE)

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
                positionX = topping.positionX + intent.deltaX,
                positionY = topping.positionY + intent.deltaY,
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

    /**
     * TODO(#274 대기): 서버 토핑은 https 주소라 편집 화면이 [android.content.ContentResolver] 로
     *  열지 못한다. 이미지를 기기에 받아 그 경로를 넘기는 일은 토핑 편집 API 연동 쪽이다.
     */
    private fun handleOnClickEditTopping() {
        val selected = state.value.toppings.find { it.parfaitImageId == state.value.selectedToppingId } ?: return

        postSideEffect(
            effect = CanvasBGEditEffect.NavigateToToppingEdit(
                toppingId = selected.parfaitImageId,
                // 서버는 잘라낸 결과만 들고 있어 원본과 누끼가 같은 그림이다
                sourceImageUri = selected.cutoutImagePath ?: selected.imageUrl,
                segmentationImageUri = selected.cutoutImagePath ?: selected.imageUrl,
                borderLayers = selected.borderLayers,
            ),
        )
    }

    private fun handleOnToppingEditResult(intent: CanvasBGEditIntent.OnToppingEditResult) {
        applyToppingTransform(intent.toppingId) { topping ->
            topping.copy(
                borderLayers = intent.result.borderLayers,
                editedImagePath = intent.result.subjectImagePath,
                cutoutImagePath = File(intent.result.cutoutImagePath).toUri().toString(),
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

    /**
     * 저장이 끝나야 화면을 넘긴다 — [CanvasBGEditEffect.ConfirmBackground] 를 먼저 쏘면 캔버스
     * 메인이 저장되지 않은 배경을 그린 채로 서 있게 되고, 다음 조회에서 슬그머니 되돌아간다.
     *
     * 토핑 저장을 배경 저장보다 먼저 완전히 기다리는 이유: 둘을 진짜 병렬로 얽으면 화면이
     * 어느 한쪽만 실패했을 때를 갈라 다뤄야 해서 복잡해진다. 토핑 저장(들끼리는 병렬)을 먼저
     * 끝내고, 그다음 기존 배경 저장 흐름을 그대로 태운다.
     */
    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY) {
            // 토핑마다 독립적인 네트워크 호출이라 순차로 기다리면 토핑 수만큼 확인 버튼이 느려진다
            state.value.toppings
                .map { topping -> async { updateToppingIfChanged(topping) } }
                .awaitAll()

            saveBackground()
        }
    }

    /**
     * [confirmedToppings]과 비교해 실제로 바뀐 것만 PATCH 한다 — 위치·크기·각도와 테두리는
     * 서버 API 자체가 갈라져 있어(`ToppingRepository.update` vs `updateBorder`) 독립적으로
     * 비교하고 독립적으로 보낸다. 일부가 403/404 등으로 실패해도 나머지 토핑·배경 확인은
     * 그대로 진행한다 — 실패 재시도 UI는 범위 밖이라 로그만 남긴다.
     */
    private suspend fun updateToppingIfChanged(topping: CanvasToppingItem) {
        val original = confirmedToppings.find { it.parfaitImageId == topping.parfaitImageId }

        val transformChanged = original == null ||
            topping.positionX != original.positionX ||
            topping.positionY != original.positionY ||
            topping.scale != original.scale ||
            topping.rotationDegrees != original.rotationDegrees
        if (transformChanged) {
            updateToppingUseCase(
                groupId = groupId,
                parfaitId = parfaitId,
                parfaitImageId = ParfaitImageId(topping.parfaitImageId),
                positionX = topping.positionX.toDouble(),
                positionY = topping.positionY.toDouble(),
                scale = topping.scale.toDouble(),
                rotation = topping.rotationDegrees.toDouble(),
            ).onFailure { throwable ->
                viewModelLogger.e(throwable) { "토핑 변형을 저장하지 못했다 - parfaitImageId: ${topping.parfaitImageId}" }
            }
        }

        val borderChanged = original == null || topping.borderLayers != original.borderLayers
        if (borderChanged) {
            updateToppingBorderUseCase(
                groupId = groupId,
                parfaitId = parfaitId,
                parfaitImageId = ParfaitImageId(topping.parfaitImageId),
                border = topping.borderLayers.toToppingBorder(),
            ).onFailure { throwable ->
                viewModelLogger.e(throwable) { "토핑 테두리를 저장하지 못했다 - parfaitImageId: ${topping.parfaitImageId}" }
            }
        }
    }

    /** 마지막 겹이 가장 바깥쪽, 즉 화면에 보이는 테두리다 — 서버는 그 한 겹만 값으로 받는다 */
    private fun List<ToppingBorderLayer>.toToppingBorder(): ToppingBorder {
        val layer = lastOrNull() ?: return ToppingBorder.None
        return ToppingBorder.Solid(color = layer.colorArgb.toRgbHexString(), width = layer.widthDp.toDouble())
    }

    private suspend fun saveBackground() {
        val current = state.value
        val imageUri = current.selectedImageUri

        // 서버 배경을 그대로 둔 채 확인만 누른 경우다 — 바뀐 것이 없어 요청할 것도 없다
        if (imageUri != null && current.selectedImageSource == null) {
            postSideEffect(effect = CanvasBGEditEffect.ConfirmBackground(YGCanvasBackground.Image(imageUri)))
            return
        }

        val background = if (imageUri == null) {
            CanvasBackgroundEdit.Color(current.selectedColor.toRgbHex())
        } else {
            CanvasBackgroundEdit.Image(
                imageId = uploadBackgroundImage(imageUri)
                    .getOrElse { throwable -> return failToSave(throwable) },
            )
        }

        changeCanvasBackgroundUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            background = background,
        ).onSuccess { saved ->
            postSideEffect(
                effect = CanvasBGEditEffect.ConfirmBackground(
                    // 이미지 배경의 URL 은 이 응답으로만 알 수 있다. 그것마저 없으면
                    // 고른 값으로 그린다 — 저장은 끝났으니 화면을 막을 이유는 없다
                    background = saved.toYGCanvasBackground()
                        ?: fallbackBackground(imageUri = imageUri, color = current.selectedColor),
                ),
            )
        }.onFailure { throwable -> failToSave(throwable) }
    }

    private suspend fun uploadBackgroundImage(imageUri: String): Result<ImageId> = uploadImageUseCase(
        uri = imageUri,
        imageType = ImageType.BACKGROUND,
    )

    private fun failToSave(throwable: Throwable) {
        viewModelLogger.e(throwable) { "배경을 저장하지 못했다 - parfaitId: ${parfaitId.value}" }
        postSideEffect(effect = CanvasBGEditEffect.ShowError(throwable.toCanvasBGEditError()))
    }

    private fun CanvasBackground?.toYGCanvasBackground(): YGCanvasBackground? = when (this) {
        null -> null
        is CanvasBackground.Color -> value.toColorOrNull()?.let(YGCanvasBackground::Solid)
        is CanvasBackground.Image -> YGCanvasBackground.Image(url)
    }

    private fun fallbackBackground(
        imageUri: String?,
        color: Color,
    ): YGCanvasBackground = imageUri
        ?.let(YGCanvasBackground::Image)
        ?: YGCanvasBackground.Solid(color)

    private fun Throwable.toCanvasBGEditError(): CanvasBGEditError = when (this) {
        is AppError.Network -> CanvasBGEditError.NETWORK
        is AppError.UnsupportedImage -> CanvasBGEditError.UNSUPPORTED_IMAGE
        else -> CanvasBGEditError.UNKNOWN
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupIdValue: Long,
            @Assisted("parfaitId") parfaitIdValue: Long,
        ): CanvasBGEditViewModel
    }

    private companion object {
        const val LOAD_CANVAS_KEY = "loadCanvas"

        const val CONFIRM_KEY = "confirm"

        const val DELETE_TOPPING_KEY = "deleteTopping"
    }
}
