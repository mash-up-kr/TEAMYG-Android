package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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

data class CanvasBGEditUiState(
    val selectedTab: CanvasEditTab = CanvasEditTab.BACKGROUND,
    // TODO: 저장된 배경이 있으면 그 배경을, 없으면 첫 팔레트 색상을 기본값으로 불러와야 함
    val selectedColor: Color = CanvasBackgroundPaletteColors.first(),
    val selectedImageUri: String? = null,
    val selectedImageSource: PictureConfirmSource? = null,
    val showQuitDialog: Boolean = false,
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
}

sealed interface CanvasBGEditEffect : UiSideEffect {
    class NavigateToCamera : CanvasBGEditEffect

    class NavigateToGallery : CanvasBGEditEffect

    class NavigateBack : CanvasBGEditEffect

    data class ConfirmBackground(
        val background: YGCanvasBackground,
    ) : CanvasBGEditEffect
}

@HiltViewModel
class CanvasBGEditViewModel
@Inject
constructor() : BaseViewModel<CanvasBGEditUiState, CanvasBGEditIntent, CanvasBGEditEffect>(
    initialState = CanvasBGEditUiState(),
) {
    init {
        viewModelLogger.i { "CanvasBGEditViewModel::init" }
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
