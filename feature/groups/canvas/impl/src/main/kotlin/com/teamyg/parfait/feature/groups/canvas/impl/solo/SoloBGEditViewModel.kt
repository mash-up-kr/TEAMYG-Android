package com.teamyg.parfait.feature.groups.canvas.impl.solo

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toRgbHex
import com.teamyg.parfait.domain.usecase.solo.ChangeSoloBackgroundUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.seededFrom
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableTexts
import com.teamyg.parfait.feature.groups.canvas.impl.util.toEditableToppingsInDrawOrder
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface SoloBGEditEffect : UiSideEffect {
    data object NavigateToCamera : SoloBGEditEffect

    data object NavigateToGallery : SoloBGEditEffect

    data object NavigateBack : SoloBGEditEffect

    /** 배경이 기기에 **저장된 뒤에만** 나간다 */
    data object Close : SoloBGEditEffect

    /** 실패 사유는 갈래 없이 한 문구다. 기기 안 저장이라 네트워크 탓일 수 없다 */
    data object ShowSaveFailed : SoloBGEditEffect
}

/**
 * 혼자 공간 배경 편집. 화면·상태·인텐트는 그룹 배경 편집([CanvasBGEditUiState],
 * [CanvasBGEditIntent])을 그대로 쓰고, 저장만 기기 안 use case 로 한다.
 */
@HiltViewModel
class SoloBGEditViewModel
@Inject
constructor(
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase,
    private val changeSoloBackground: ChangeSoloBackgroundUseCase,
) : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, SoloBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    /**
     * 선택(배경)은 최초 방출에만 시딩한다 — 이후 방출이 사용자가 방금 고른 배경을 되돌리면 안 된다.
     * 토핑·텍스트는 매번 갈아 끼운다.
     */
    private var hasSeededFromCanvas = false

    init {
        viewModelLogger.i { "SoloBGEditViewModel::init" }
        observeCanvas()
    }

    private fun observeCanvas() {
        launchWhileSubscribed(source = { getSoloCanvasFlow() }) { result ->
            result
                .onSuccess { canvas ->
                    val toppings = canvas.toppings.toEditableToppingsInDrawOrder()
                    val texts = canvas.texts.toEditableTexts()
                    val seeded = hasSeededFromCanvas.not()
                    updateState {
                        val base = if (seeded) seededFrom(canvas.background) else this
                        base.copy(toppings = toppings, texts = texts)
                    }
                    hasSeededFromCanvas = true
                }
                .onFailure { throwable ->
                    // 배경은 못 읽어도 고를 수 있어 화면을 막지 않는다
                    viewModelLogger.e(throwable) { "혼자 공간 캔버스를 읽지 못했다" }
                }
        }
    }

    override fun processIntent(intent: CanvasBGEditIntent) {
        when (intent) {
            is CanvasBGEditIntent.OnSelectColor -> updateState {
                copy(selectedColor = intent.color, selectedImageUri = null, selectedImageSource = null)
            }

            is CanvasBGEditIntent.OnBackgroundImageResult -> updateState {
                copy(selectedImageUri = intent.uri, selectedImageSource = intent.source)
            }

            CanvasBGEditIntent.OnClickCamera -> postSideEffect(SoloBGEditEffect.NavigateToCamera)
            CanvasBGEditIntent.OnClickGallery -> postSideEffect(SoloBGEditEffect.NavigateToGallery)
            CanvasBGEditIntent.OnClickCloseButton -> updateState { copy(showQuitDialog = true) }
            CanvasBGEditIntent.OnQuitDialogConfirm -> postSideEffect(SoloBGEditEffect.NavigateBack)
            CanvasBGEditIntent.OnQuitDialogCancel -> updateState { copy(showQuitDialog = false) }
            CanvasBGEditIntent.OnClickConfirm -> handleOnClickConfirm()
        }
    }

    /** 저장이 끝나야 닫는다. 실패하면 화면에 남아 다시 고를 수 있다 */
    private fun handleOnClickConfirm() {
        launch(key = CONFIRM_KEY, onError = { throwable -> failToSave(throwable) }) {
            updateState { copy(isLoading = true) }

            val saved = save()

            updateState { copy(isLoading = false) }
            postSideEffect(if (saved) SoloBGEditEffect.Close else SoloBGEditEffect.ShowSaveFailed)
        }
    }

    private suspend fun save(): Boolean {
        val current = state.value
        val imageUri = current.selectedImageUri

        // 저장돼 있던 이미지 배경을 그대로 둔 채 확인만 눌렀다 — 바뀐 것이 없다
        if (imageUri != null && current.selectedImageSource == null) return true

        val result = if (imageUri == null) {
            changeSoloBackground.color(current.selectedColor.toRgbHex())
        } else {
            changeSoloBackground.image(imageUri)
        }

        return result
            .onFailure { throwable -> viewModelLogger.e(throwable) { "혼자 공간 배경을 저장하지 못했다" } }
            .isSuccess
    }

    private fun failToSave(throwable: Throwable) {
        viewModelLogger.e(throwable) { "혼자 공간 배경 저장 중 예기치 못한 실패" }
        updateState { copy(isLoading = false) }
        postSideEffect(SoloBGEditEffect.ShowSaveFailed)
    }

    private companion object {
        const val CONFIRM_KEY = "confirm"
    }
}
