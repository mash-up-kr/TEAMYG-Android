package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
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
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.usecase.image.UploadImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ChangeCanvasBackgroundUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTopping
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

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
 * @param toppings 캔버스에 놓인 토핑. 이 화면에서는 고칠 수 없고 배경 위에 보이기만 한다.
 */
data class CanvasBGEditUiState(
    val selectedColor: Color = CanvasBackgroundPaletteColors.first(),
    val selectedImageUri: String? = null,
    val selectedImageSource: PictureConfirmSource? = null,
    val showQuitDialog: Boolean = false,
    val toppings: List<EditableTopping> = emptyList(),
    val isLoading: Boolean = false,
) : UiState

sealed interface CanvasBGEditIntent : UiIntent {
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
}

@HiltViewModel(assistedFactory = CanvasBGEditViewModel.Factory::class)
class CanvasBGEditViewModel
@AssistedInject
constructor(
    @Assisted("groupId") groupIdValue: Long,
    @Assisted("parfaitId") parfaitIdValue: Long,
    private val getTodayParfaitFlowUseCase: GetTodayParfaitFlowUseCase,
    private val refreshTodayParfaitDetailUseCase: RefreshTodayParfaitDetailUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val changeCanvasBackgroundUseCase: ChangeCanvasBackgroundUseCase,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    private val groupId = GroupId(groupIdValue)

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

            updateState { withCanvas(canvas).copy(toppings = incoming) }
            hasSeededFromCanvas = true
        }
    }

    /**
     * 토핑 목록만 매번 갈아 끼우고 나머지는 **최초 방출에만** 시딩한다 — 이후 방출까지 대입하면
     * 사용자가 방금 고른 배경이 되돌아간다.
     *
     * 저장된 배경 색을 못 읽으면 기본 색으로 두는데, 그때 확인을 누르면 배경이 팔레트 첫 색으로
     * 바뀐다 — 못 읽는 색을 그대로 되돌려 보내는 것보다 낫다.
     */
    private fun CanvasBGEditUiState.withCanvas(canvas: CanvasVO): CanvasBGEditUiState {
        if (hasSeededFromCanvas) return this

        return copy(
            selectedColor = (canvas.background as? CanvasBackground.Color)
                ?.value
                ?.toColorOrNull()
                ?: selectedColor,
            selectedImageUri = (canvas.background as? CanvasBackground.Image)?.url,
            selectedImageSource = null,
        )
    }

    override fun processIntent(intent: CanvasBGEditIntent) {
        when (intent) {
            is CanvasBGEditIntent.OnSelectColor -> handleOnSelectColor(intent)
            is CanvasBGEditIntent.OnBackgroundImageResult -> handleOnBackgroundImageResult(intent)
            CanvasBGEditIntent.OnClickCamera -> postSideEffect(effect = CanvasBGEditEffect.NavigateToCamera)
            CanvasBGEditIntent.OnClickGallery -> postSideEffect(effect = CanvasBGEditEffect.NavigateToGallery)
            CanvasBGEditIntent.OnClickCloseButton -> updateState { copy(showQuitDialog = true) }
            CanvasBGEditIntent.OnQuitDialogConfirm -> postSideEffect(effect = CanvasBGEditEffect.NavigateBack)
            CanvasBGEditIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }
            CanvasBGEditIntent.OnClickConfirm -> handleOnClickConfirm()
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
     */
    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY, onError = ::failToSaveUnexpectedly) {
            updateState { copy(isLoading = true) }

            val savedBackground = saveBackground()

            updateState { copy(isLoading = false) }

            // 실패 토스트는 saveBackground() 가 이미 내보냈다
            if (savedBackground != null) {
                postSideEffect(effect = CanvasBGEditEffect.ConfirmBackground(savedBackground))
            }
        }
    }

    private fun failToSaveUnexpectedly(throwable: Throwable) {
        viewModelLogger.e(throwable) { "편집을 저장하지 못했다 - parfaitId: ${parfaitId.value}" }
        updateState { copy(isLoading = false) }
        postSideEffect(effect = CanvasBGEditEffect.ShowError(throwable.toCanvasBGEditError()))
    }

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
        else -> CanvasBGEditError.BACKGROUND_SAVE_UNKNOWN
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("groupId") groupIdValue: Long,
            @Assisted("parfaitId") parfaitIdValue: Long,
        ): CanvasBGEditViewModel
    }

    private companion object {
        const val CONFIRM_KEY = "confirm"
    }
}
