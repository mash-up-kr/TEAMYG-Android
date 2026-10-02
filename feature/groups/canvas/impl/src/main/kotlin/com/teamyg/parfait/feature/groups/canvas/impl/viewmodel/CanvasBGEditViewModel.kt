package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
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
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.usecase.image.UploadImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ChangeCanvasBackgroundUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingsUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingBorderStyle
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_MIN_SCALE
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingBorder
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

enum class CanvasEditTab { BACKGROUND, TOPPING }

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
    val toppings: List<EditableTopping> = emptyList(),
    val selectedToppingId: Long? = null,
    val showDeleteToppingDialog: Boolean = false,
    /**
     * 아직 서버에 반영되지 않은 로컬 변경. 이동·크기조절·회전·테두리 편집이 여기 든다.
     *
     * **삭제는 넣지 않는다** — 삭제 모달의 확인이 곧 DELETE 라 이미 서버에 반영돼 있고,
     * 확인 버튼은 삭제를 다루지 않는다.
     *
     * 같은 화면의 [selectedToppingId] 가 `Long` 이라 그것에 맞춘다. `ParfaitImageId` 로 감싸는
     * 자리는 지금처럼 API 호출 직전 한 곳뿐이다.
     */
    val dirtyToppingIds: Set<Long> = emptySet(),
    /**
     * 지운 토핑의 툼스톤. 삭제 직전에 출발한 갱신 응답이 뒤늦게 도착하면 그 토핑이 아직 서버
     * 목록에 있어서, 이게 없으면 방금 지운 토핑이 되살아난다.
     */
    val deletedToppingIds: Set<Long> = emptySet(),
    /** 확인과 삭제가 한 깃발을 나눠 쓴다 — 덮개가 입력을 삼켜 둘이 겹칠 수 없다. */
    val isLoading: Boolean = false,
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
        val topping: EditableTopping,
    ) : CanvasBGEditIntent

    /** 딤 처리된 영역(배경, 남의 토핑)을 탭해 지금 선택을 해제한다. */
    data object OnClickDeselectTopping : CanvasBGEditIntent

    /** 툴바의 삭제 버튼. 바로 지우지 않고 확인 모달을 띄운다. */
    data object OnClickDeleteToppingButton : CanvasBGEditIntent

    data object OnDeleteToppingDialogConfirm : CanvasBGEditIntent

    data object OnDeleteToppingDialogCancel : CanvasBGEditIntent

    data object OnClickEditTopping : CanvasBGEditIntent

    /** `panX`/`panY` 는 px 가 아니라 Canvas-Area 대비 비율이다. */
    data class OnToppingTransform(
        val panX: Float,
        val panY: Float,
        val zoom: Float,
        val rotationDelta: Float,
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
    @Assisted("initialToppingId") initialToppingIdValue: Long?,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val refreshTodayParfaitDetailUseCase: RefreshTodayParfaitDetailUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val changeCanvasBackgroundUseCase: ChangeCanvasBackgroundUseCase,
    private val deleteToppingUseCase: DeleteToppingUseCase,
    private val updateToppingsUseCase: UpdateToppingsUseCase,
    private val updateToppingBorderUseCase: UpdateToppingBorderUseCase,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    /** 특정 토핑을 탭해 들어온 경우 그 토핑 id. 첫 로드에서 토핑 탭·선택 상태를 여기서 채운다 */
    private val initialToppingId = initialToppingIdValue

    /**
     * 배경을 저장할 대상. 캔버스 메인이 열어 준 오늘의 캔버스로 시작하지만, 최초 방출이 다른
     * parfaitId 를 주면 그쪽으로 옮긴다([hasSeededFromCanvas] 참고) — 화면에 그려진 토핑과
     * 저장 대상이 갈라지는 편이 더 나쁘다.
     *
     * 그 뒤 날이 바뀌어 조회가 다른 날의 캔버스를 주는 경우는 여기서 옮기지 않는다 — 이 화면의
     * 시간 축이 닫을 몫이다(`specs/2026-08-27-canvas-today-ssot-polling.md` 「하루 경계」).
     */
    private var parfaitId = ParfaitId(parfaitIdValue)

    /**
     * 최초 방출에만 서버 값을 시딩하고, 편집 대상([parfaitId])도 최초 방출로만 정한다 — 이후
     * 방출이 사용자의 선택을 덮거나, 화면에 그려진 토핑과 다른 캔버스로 저장 대상을 바꾸면
     * 안 된다.
     */
    private var hasSeededFromCanvas = false

    /**
     * 서버가 마지막으로 준 그대로의 토핑. 확인 때 손댄 토핑의 **어느 축**이 바뀌었는지 가리는 데만
     * 쓴다 — 화면 렌더링에는 [CanvasBGEditUiState.toppings] 를 본다.
     */
    private var serverToppings: List<EditableTopping> = emptyList()

    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
        observeCanvas()
    }

    /**
     * 화면이 보이는 동안만 오늘 캔버스를 구독한다. 갱신은 구독 시작 즉시, 이후 주기마다
     * 폴러가 맡는다(`adr/0029-canvas-today-ssot-polling.md`) — 여기서 따로 조회를 걸지 않는다.
     */
    private fun observeCanvas() {
        launchWhileSubscribed(source = { getTodayParfaitFlowUseCase(groupId) }) { canvas ->
            if (canvas == null) return@launchWhileSubscribed

            if (hasSeededFromCanvas.not() && canvas.parfaitId != parfaitId) {
                viewModelLogger.e {
                    "편집을 연 캔버스와 조회 결과가 다르다 — 조회 쪽으로 옮긴다" +
                        " (열린 것: ${parfaitId.value}, 받은 것: ${canvas.parfaitId.value})"
                }
                parfaitId = canvas.parfaitId
            }

            val incoming = canvas.toppings
                .sortedBy { topping -> topping.transform.positionZ }
                .map { topping -> topping.toEditableTopping() }

            serverToppings = incoming
            updateState { withCanvas(canvas).mergeToppings(incoming) }
            hasSeededFromCanvas = true
        }
    }

    /**
     * 토핑 목록만 매번 갈아 끼우고 나머지는 **최초 방출에만** 시딩한다 — 이후 방출까지 대입하면
     * 사용자가 방금 고른 배경이 되돌아가고, 옮긴 탭과 푼 선택도 다음 갱신에 되돌아간다.
     *
     * 저장된 배경 색을 못 읽으면 기본 색으로 두는데, 그때 확인을 누르면 배경이 팔레트 첫 색으로
     * 바뀐다 — 못 읽는 색을 그대로 되돌려 보내는 것보다 낫다.
     */
    private fun CanvasBGEditUiState.withCanvas(canvas: CanvasVO): CanvasBGEditUiState {
        if (hasSeededFromCanvas) return this

        return copy(
            selectedTab = if (initialToppingId != null) CanvasEditTab.TOPPING else selectedTab,
            selectedColor = (canvas.background as? CanvasBackground.Color)
                ?.value
                ?.toColorOrNull()
                ?: selectedColor,
            selectedImageUri = (canvas.background as? CanvasBackground.Image)?.url,
            selectedImageSource = null,
            selectedToppingId = initialToppingId ?: selectedToppingId,
        )
    }

    /**
     * 최초 방출도 예외가 아니다 — 그때는 두 집합이 비어 있어 결과가 통째 대입과 같아진다.
     * 화면은 그 방출이 폴링에서 왔는지 강제 갱신에서 왔는지 구분하지 않는다.
     */
    private fun CanvasBGEditUiState.mergeToppings(incoming: List<EditableTopping>): CanvasBGEditUiState {
        val incomingIds = incoming.mapTo(mutableSetOf()) { it.parfaitImageId }
        val localById = toppings.associateBy { it.parfaitImageId }

        val merged = incoming
            .filterNot { it.parfaitImageId in deletedToppingIds }
            .map { server ->
                if (server.parfaitImageId in dirtyToppingIds) localById[server.parfaitImageId] ?: server else server
            }

        return copy(
            toppings = merged,
            // 서버 목록에서 사라진 것은 두 집합에서도 뺀다 — 없는 토핑에 PATCH 를 보낼 수 없고,
            // 툼스톤도 제 역할을 다했다
            dirtyToppingIds = dirtyToppingIds intersect incomingIds,
            deletedToppingIds = deletedToppingIds intersect incomingIds,
            selectedToppingId = selectedToppingId?.takeIf { it in incomingIds && it !in deletedToppingIds },
        )
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
            is CanvasBGEditIntent.OnToppingTransform -> handleOnToppingTransform(intent)
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

        launch(key = DELETE_TOPPING_KEY, onError = { failToDeleteTopping(it, selectedId) }) {
            // 상태를 블록 밖이 아니라 안에서 바꾼다 — `launch` 가 중복 호출을 걸러 아무것도
            // 시작하지 않는 경우까지 모달이 닫히고 덮개만 남으면 안 된다
            updateState { copy(showDeleteToppingDialog = false, isLoading = true) }

            deleteToppingUseCase(groupId, parfaitId, ParfaitImageId(selectedId))
                .onSuccess {
                    updateState {
                        copy(
                            toppings = toppings.filterNot { it.parfaitImageId == selectedId },
                            deletedToppingIds = deletedToppingIds + selectedId,
                            dirtyToppingIds = dirtyToppingIds - selectedId,
                            selectedToppingId = null,
                            isLoading = false,
                        )
                    }
                    // 되감기 전에 기다린다 — 먼저 나가면 라우트가 되감기며 viewModelScope 가
                    // 취소돼 갱신이 끊긴다
                    refreshTodayParfaitDetailUseCase(groupId = groupId, parfaitId = parfaitId)

                    postSideEffect(effect = CanvasBGEditEffect.NavigateBack)
                }.onFailure { throwable -> failToDeleteTopping(throwable, selectedId) }
        }
    }

    private fun failToDeleteTopping(
        throwable: Throwable,
        toppingId: Long,
    ) {
        viewModelLogger.e(throwable) { "토핑을 지우지 못했다 - parfaitImageId: $toppingId" }
        updateState { copy(isLoading = false) }
        postSideEffect(
            effect = CanvasBGEditEffect.ShowError(
                throwable.toCanvasBGEditError(unknown = CanvasBGEditError.TOPPING_DELETE_UNKNOWN),
            ),
        )
    }

    private fun handleOnToppingTransform(intent: CanvasBGEditIntent.OnToppingTransform) {
        val selectedId = state.value.selectedToppingId ?: return

        applyToppingTransform(selectedId) { topping ->
            topping.copy(
                positionX = topping.positionX + intent.panX,
                positionY = topping.positionY + intent.panY,
                scale = (topping.scale * intent.zoom).coerceAtLeast(TOPPING_MIN_SCALE),
                rotationDegrees = topping.rotationDegrees + intent.rotationDelta,
            )
        }
    }

    /**
     * [transform]으로 크기/회전/위치를 바꾼다. 캔버스 밖으로 나가는 부분은 화면에서 클립되어 안 보이므로
     * 여기서 위치를 되돌리지 않고 [transform] 결과를 그대로 반영한다.
     */
    private fun applyToppingTransform(
        toppingId: Long,
        transform: (EditableTopping) -> EditableTopping,
    ) {
        updateState {
            copy(
                toppings = toppings.map { topping ->
                    if (topping.parfaitImageId != toppingId) topping else transform(topping)
                },
            ).markDirty(toppingId)
        }
    }

    private fun CanvasBGEditUiState.markDirty(toppingId: Long): CanvasBGEditUiState =
        copy(dirtyToppingIds = dirtyToppingIds + toppingId)

    /**
     * 서버 토핑은 https 주소라 [android.content.ContentResolver] 로 열지 못하지만,
     * `ImageDownloadRemoteDataSource` 가 그 스킴을 갈라 기기에 받아 두므로 편집 화면은
     * 이 URL 을 그대로 받아도 된다(`ImageSegmentationRepositoryImpl.decodeImage`).
     */
    private fun handleOnClickEditTopping() {
        val selected = state.value.toppings.find { it.parfaitImageId == state.value.selectedToppingId } ?: return

        postSideEffect(
            effect = CanvasBGEditEffect.NavigateToToppingEdit(
                toppingId = selected.parfaitImageId,
                // 서버는 잘라낸 결과만 들고 있어 원본과 누끼가 같은 그림이다
                sourceImageUri = selected.cutoutImagePath ?: selected.imageUrl,
                segmentationImageUri = selected.cutoutImagePath ?: selected.imageUrl,
                borderLayers = listOfNotNull(selected.border?.let { ToppingBorderLayer(it.colorArgb, it.widthDp) }),
            ),
        )
    }

    private fun handleOnToppingEditResult(intent: CanvasBGEditIntent.OnToppingEditResult) {
        // 테두리 PATCH 는 아직 소비처가 없지만(OQ-P-276) 이 토핑의 로컬 값이 갱신에 덮이면
        // 안 되는 것은 같다 — applyToppingTransform 이 함께 미는 위치 PATCH 는 무해하다
        applyToppingTransform(intent.toppingId) { topping ->
            topping.copy(
                border = intent.result.borderLayers
                    .lastOrNull()
                    ?.let { ToppingBorderStyle(it.colorArgb, it.widthDp) },
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
     * 토핑 저장을 배경 저장보다 먼저 완전히 기다리는 이유: 둘을 진짜 병렬로 얽으면 어느 쪽이
     * 실패했는지에 따라 화면을 닫을지 정하는 아래 분기를 짤 수 없다.
     */
    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY, onError = ::failToSaveUnexpectedly) {
            updateState { copy(isLoading = true) }

            val failedToppingIds = updateDirtyToppings()
            // 보낸 것만 대상에서 뺀다 — 여기서 통째 비우면 다시 누른 확인이 못 보낸 토핑을 건너뛴다
            updateState { copy(dirtyToppingIds = failedToppingIds) }

            val savedBackground = saveBackground()

            updateState { copy(isLoading = false) }

            when {
                // 실패 토스트는 saveBackground() 가 이미 내보냈다
                savedBackground == null -> Unit

                // 닫으면 사용자는 방금 옮긴 토핑이 되돌아간 캔버스를 보게 된다
                failedToppingIds.isNotEmpty() -> postSideEffect(
                    effect = CanvasBGEditEffect.ShowError(CanvasBGEditError.TOPPING_SAVE_UNKNOWN),
                )

                else -> postSideEffect(effect = CanvasBGEditEffect.ConfirmBackground(savedBackground))
            }
        }
    }

    private fun failToSaveUnexpectedly(throwable: Throwable) {
        viewModelLogger.e(throwable) { "편집을 저장하지 못했다 - parfaitId: ${parfaitId.value}" }
        updateState { copy(isLoading = false) }
        postSideEffect(
            effect = CanvasBGEditEffect.ShowError(
                throwable.toCanvasBGEditError(unknown = CanvasBGEditError.BACKGROUND_SAVE_UNKNOWN),
            ),
        )
    }

    /**
     * PATCH 대상은 지금 목록에 있으면서 손댄 토핑뿐이다. 대조를 dirty 안에서만 하는 것이 요점이다:
     * 목록 전체를 스냅샷과 견주면 갱신이 들여온 남의 새 토핑이 "스냅샷에 없음 = 바뀜"으로 잡힌다.
     *
     * 축으로 가르는 것은 서버 API 가 갈라져 있어서다 — 변형만 한 요청에 접힌다.
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun updateDirtyToppings(): Set<Long> = coroutineScope {
        val current = state.value
        val dirty = current.toppings.filter { it.parfaitImageId in current.dirtyToppingIds }

        val transformChanged = dirty.filter { it.hasTransformChange() }
        val borderChanged = dirty.filter { it.hasBorderChange() }

        val transformFailures = saveTransforms(transformChanged)
        val borderFailures = borderChanged
            .map { topping -> async { topping.parfaitImageId.takeIf { saveBorder(topping).not() } } }
            .awaitAll()
            .filterNotNull()

        transformFailures + borderFailures
    }

    /**
     * 일괄이라 부분 성공이 없어서, 실패하면 보낸 토핑 전부를 대상으로 남긴다. 되풀이되는 실패가
     * 섞이면 나머지까지 계속 막히는 것을 감수한 설계다 —
     * `specs/2026-08-31-topping-batch-update-and-past-canvas-status.md` 「주의 / 열린 질문」 절.
     *
     * @return 저장하지 못한 토핑의 id.
     */
    private suspend fun saveTransforms(toppings: List<EditableTopping>): Set<Long> {
        if (toppings.isEmpty()) return emptySet()

        return updateToppingsUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            updates = toppings.map { it.toTransformUpdate() },
        ).fold(
            onSuccess = { emptySet() },
            onFailure = { throwable ->
                viewModelLogger.e(throwable) { "토핑 변형을 저장하지 못했다 - ${toppings.map { it.parfaitImageId }}" }
                toppings.mapTo(mutableSetOf()) { it.parfaitImageId }
            },
        )
    }

    private suspend fun saveBorder(topping: EditableTopping): Boolean = updateToppingBorderUseCase(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = ParfaitImageId(topping.parfaitImageId),
        border = topping.border.toToppingBorder(),
    ).onFailure { throwable ->
        viewModelLogger.e(throwable) { "토핑 테두리를 저장하지 못했다 - ${topping.parfaitImageId}" }
    }.isSuccess

    /** 스냅샷에 없는 토핑을 바뀐 것으로 봐도 안전하다 — 갱신이 들여온 토핑은 dirty 에 안 들어온다. */
    private fun EditableTopping.hasTransformChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return positionX != original.positionX ||
            positionY != original.positionY ||
            scale != original.scale ||
            rotationDegrees != original.rotationDegrees
    }

    private fun EditableTopping.hasBorderChange(): Boolean {
        val original = serverToppings.find { it.parfaitImageId == parfaitImageId } ?: return true
        return border != original.border
    }

    /** 겹침 순서는 안 보낸다 — 앱에 z 조작 경로가 없어 서버 값을 그대로 둔다. */
    private fun EditableTopping.toTransformUpdate(): ToppingTransformUpdate = ToppingTransformUpdate(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        positionX = positionX.toDouble(),
        positionY = positionY.toDouble(),
        scale = scale.toDouble(),
        rotation = rotationDegrees.toDouble(),
    )

    /**
     * @return 저장된 배경. 실패하면 `null` 이고, 그때 실패 토스트는 여기서 이미 내보냈다.
     */
    private suspend fun saveBackground(): YGCanvasBackground? {
        val current = state.value
        val imageUri = current.selectedImageUri

        // 서버 배경을 그대로 둔 채 확인만 누른 경우다 — 바뀐 것이 없어 요청할 것도 없다
        if (imageUri != null && current.selectedImageSource == null) {
            return YGCanvasBackground.Image(imageUri)
        }

        val background = if (imageUri == null) {
            CanvasBackgroundEdit.Color(current.selectedColor.toRgbHex())
        } else {
            CanvasBackgroundEdit.Image(
                imageId = uploadBackgroundImage(imageUri)
                    .getOrElse { throwable ->
                        failToSave(throwable)
                        return null
                    },
            )
        }

        return changeCanvasBackgroundUseCase(
            groupId = groupId,
            parfaitId = parfaitId,
            background = background,
        ).fold(
            onSuccess = { saved ->
                // 되감기 전에 기다린다 — ConfirmBackground 를 먼저 쏘면 라우트가 되감기며
                // viewModelScope 가 취소돼 launch 로 건 갱신이 끊긴다
                refreshTodayParfaitDetailUseCase(groupId = groupId, parfaitId = parfaitId)

                // 이미지 배경의 URL 은 이 응답으로만 알 수 있다. 그것마저 없으면
                // 고른 값으로 그린다 — 저장은 끝났으니 화면을 막을 이유는 없다
                saved.toYGCanvasBackground()
                    ?: fallbackBackground(imageUri = imageUri, color = current.selectedColor)
            },
            onFailure = { throwable ->
                failToSave(throwable)
                null
            },
        )
    }

    private suspend fun uploadBackgroundImage(imageUri: String): Result<ImageId> = uploadImageUseCase(
        uri = imageUri,
        imageType = ImageType.BACKGROUND,
    )

    private fun failToSave(throwable: Throwable) {
        viewModelLogger.e(throwable) { "배경을 저장하지 못했다 - parfaitId: ${parfaitId.value}" }
        postSideEffect(
            effect = CanvasBGEditEffect.ShowError(
                throwable.toCanvasBGEditError(unknown = CanvasBGEditError.BACKGROUND_SAVE_UNKNOWN),
            ),
        )
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

    /**
     * @param unknown 갈래를 가리지 못했을 때 쓸 값. 무엇을 하다 실패했는지는 부르는 쪽만 안다.
     */
    private fun Throwable.toCanvasBGEditError(unknown: CanvasBGEditError): CanvasBGEditError = when (this) {
        is AppError.Network -> CanvasBGEditError.NETWORK
        is AppError.UnsupportedImage -> CanvasBGEditError.UNSUPPORTED_IMAGE
        else -> unknown
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupIdValue: Long,
            @Assisted("parfaitId") parfaitIdValue: Long,
            @Assisted("initialToppingId") initialToppingIdValue: Long?,
        ): CanvasBGEditViewModel
    }

    private companion object {
        const val CONFIRM_KEY = "confirm"

        const val DELETE_TOPPING_KEY = "deleteTopping"
    }
}
