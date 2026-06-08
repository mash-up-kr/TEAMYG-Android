package com.teamyg.gallery.impl.viewmodel

import androidx.compose.runtime.Immutable
import com.teamyg.gallery.impl.model.GalleryAccessLevel
import com.teamyg.gallery.impl.model.GalleryImageGroup
import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Immutable
data class CustomGalleryPickerState(
    val isLoading: Boolean = false,
    val access: GalleryAccessLevel = GalleryAccessLevel.INITIAL,
    val groups: List<GalleryImageGroup> = emptyList(),
) : UiState {
    val isEmpty: Boolean
        get() = groups.all { it.images.isEmpty() }
}

sealed class CustomGalleryPickerEffect private constructor() : UiSideEffect {
    data object RequestPermission : CustomGalleryPickerEffect()

    data object OpenAppSettings : CustomGalleryPickerEffect()

    data object LoadImages : CustomGalleryPickerEffect()

    data class ReturnResult(
        val uri: String,
    ) : CustomGalleryPickerEffect()

    data object NavigateToBack : CustomGalleryPickerEffect()
}

sealed class CustomGalleryPickerIntent private constructor() : UiIntent {
    data class OnPermissionResult(
        val access: GalleryAccessLevel,
    ) : CustomGalleryPickerIntent()

    data object OnRequestPermission : CustomGalleryPickerIntent()

    data object OnRequestOpenSettings : CustomGalleryPickerIntent()

    data object OnRequestManageMedia : CustomGalleryPickerIntent()

    data class OnImagesLoaded(
        val groups: List<GalleryImageGroup>,
    ) : CustomGalleryPickerIntent()

    data class OnClickImage(
        val uri: String,
    ) : CustomGalleryPickerIntent()

    data object OnCancel : CustomGalleryPickerIntent()
}

@HiltViewModel
class CustomGalleryPickerViewModel
@Inject
constructor() : BaseViewModel<CustomGalleryPickerState, CustomGalleryPickerIntent, CustomGalleryPickerEffect>(
    initialState = CustomGalleryPickerState(),
) {
    override fun processIntent(intent: CustomGalleryPickerIntent) {
        when (intent) {
            is CustomGalleryPickerIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomGalleryPickerIntent.OnRequestPermission -> handleOnRequestPermission()
            is CustomGalleryPickerIntent.OnRequestOpenSettings -> handleOnRequestOpenSettings()
            is CustomGalleryPickerIntent.OnRequestManageMedia -> handleOnRequestManageMedia()
            is CustomGalleryPickerIntent.OnImagesLoaded -> handleOnImagesLoaded(intent)
            is CustomGalleryPickerIntent.OnClickImage -> handleOnClickImage(intent)
            is CustomGalleryPickerIntent.OnCancel -> handleOnCancel()
        }
    }

    private fun handleOnPermissionResult(intent: CustomGalleryPickerIntent.OnPermissionResult) {
        val access: GalleryAccessLevel = intent.access

        updateState { copy(access = access) }

        if (access == GalleryAccessLevel.PARTIAL || access == GalleryAccessLevel.FULL) {
            updateState { copy(isLoading = true) }
            postSideEffect(CustomGalleryPickerEffect.LoadImages)
        }
    }

    private fun handleOnRequestPermission() {
        postSideEffect(CustomGalleryPickerEffect.RequestPermission)
    }

    private fun handleOnRequestOpenSettings() {
        postSideEffect(CustomGalleryPickerEffect.OpenAppSettings)
    }

    private fun handleOnRequestManageMedia() {
        postSideEffect(CustomGalleryPickerEffect.RequestPermission)
    }

    private fun handleOnImagesLoaded(intent: CustomGalleryPickerIntent.OnImagesLoaded) {
        updateState {
            copy(
                isLoading = false,
                groups = intent.groups,
            )
        }
    }

    private fun handleOnClickImage(intent: CustomGalleryPickerIntent.OnClickImage) {
        postSideEffect(CustomGalleryPickerEffect.ReturnResult(intent.uri))
    }

    private fun handleOnCancel() {
        postSideEffect(CustomGalleryPickerEffect.NavigateToBack)
    }
}
