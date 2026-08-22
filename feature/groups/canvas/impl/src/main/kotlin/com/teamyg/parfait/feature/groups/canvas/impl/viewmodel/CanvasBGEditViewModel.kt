package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.util.resizeOutwardDirection
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.io.File

enum class CanvasEditTab { BACKGROUND, TOPPING }

data class CanvasToppingItem(
    val parfaitImageId: Long,
    val isMine: Boolean,
    val imageUrl: String,
    /** 캔버스 대비 0~1 로 정규화된 중심점. 메인 캔버스([CanvasToppingVO.transform])와 같은 기준이다 */
    val positionX: Float,
    val positionY: Float,
    val sourceImageUri: String,
    val segmentationImageUri: String,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    /** 테두리 편집을 거쳐 새로 구운 이미지 경로. 있으면 [imageUrl] 대신 이걸 그린다. */
    val editedImagePath: String? = null,
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

    /** 선택된 토핑 자신을 잡고 드래그한 만큼 넘어온다. 캔버스 대비 비율 델타로 위치를 옮긴다. */
    data class OnToppingMoveDrag(
        val delta: Offset,
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

@HiltViewModel(assistedFactory = CanvasBGEditViewModel.Factory::class)
class CanvasBGEditViewModel
@AssistedInject
constructor(
    @Assisted("groupId") groupIdValue: Long,
    @Assisted("parfaitId") parfaitIdValue: Long,
    private val getParfaitDetailUseCase: GetParfaitDetailUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val refreshGroupDetailUseCase: RefreshGroupDetailUseCase,
    private val deleteToppingUseCase: DeleteToppingUseCase,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    private val groupId = GroupId(groupIdValue)
    private val parfaitId = ParfaitId(parfaitIdValue)

    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
        loadToppings()
    }

    private fun loadToppings() {
        launch(key = LOAD_TOPPINGS_KEY) {
            val myNickname = myGroupNickname()

            getParfaitDetailUseCase(groupId, parfaitId)
                .onSuccess { canvas ->
                    val myGroupMemberId = canvas.members
                        .firstOrNull { it.nickname == myNickname }
                        ?.groupMemberId

                    updateState {
                        copy(
                            toppings = canvas.toppings
                                .sortedBy { it.transform.positionZ }
                                .map { it.toCanvasToppingItem(isMine = it.placedBy.groupMemberId == myGroupMemberId) },
                        )
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "토핑 목록을 불러오지 못했다 - parfaitId: ${parfaitId.value}" }
                }
        }
    }

    /**
     * "내 토핑"을 가리려면 이 그룹에서의 내 groupMemberId 가 필요한데, 그걸 직접 주는 API가 없다.
     * 대신 [ParfaitGroupDetailVO.groupNickname]("인증 회원 본인이 이 그룹에서 쓰는 이름")이
     * 캔버스 응답의 members 안 내 항목과 같은 값이라는 점을 이용해 닉네임으로 대조한다.
     *
     * 캐시를 먼저 보지 않고 매번 새로고침한다 — 편집 화면은 자주 열리지 않아 정확성이 우선이다.
     */
    private suspend fun myGroupNickname(): GroupNickname? {
        refreshGroupDetailUseCase(groupId).onFailure { throwable ->
            viewModelLogger.e(throwable) { "그룹 상세를 새로고침하지 못했다 - groupId: ${groupId.value}" }
        }
        return getGroupDetailUseCase(groupId).first()?.groupNickname
    }

    private fun CanvasToppingVO.toCanvasToppingItem(isMine: Boolean) = CanvasToppingItem(
        parfaitImageId = parfaitImageId.value,
        isMine = isMine,
        imageUrl = imageUrl,
        positionX = transform.positionX.toFloat(),
        positionY = transform.positionY.toFloat(),
        sourceImageUri = imageUrl,
        segmentationImageUri = imageUrl,
        scale = transform.scale.toFloat(),
        rotationDegrees = transform.rotation.toFloat(),
    )

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
                positionX = topping.positionX + intent.delta.x,
                positionY = topping.positionY + intent.delta.y,
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

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupIdValue: Long,
            @Assisted("parfaitId") parfaitIdValue: Long,
        ): CanvasBGEditViewModel
    }

    private companion object {
        const val LOAD_TOPPINGS_KEY = "loadToppings"
        const val DELETE_TOPPING_KEY = "deleteTopping"
    }
}
