package com.teamyg.gallery.impl.viewmodel

import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SystemGalleryState(
    val imageUri: String? = null,
) : UiState

sealed interface SystemGalleryIntent : UiIntent {
    data class PickPhoto(val imageUri: String?) : SystemGalleryIntent

    data class ConfirmPhoto(val imageUri: String?) : SystemGalleryIntent
}

sealed interface SystemGallerySideEffect : UiSideEffect {
    data class NavigateToBack(val imageUri: String?) : SystemGallerySideEffect
}

@HiltViewModel
class SystemGalleryPickerViewModel
@Inject
constructor() : BaseViewModel<SystemGalleryState, SystemGalleryIntent, SystemGallerySideEffect>(
    initialState = SystemGalleryState(),
) {
    override fun processIntent(intent: SystemGalleryIntent) {
        when (intent) {
            is SystemGalleryIntent.PickPhoto -> {
                if (intent.imageUri != null) {
                    updateState { copy(imageUri = intent.imageUri) }
                } else {
                    // 이미지 가져오기 실패
                }
            }

            is SystemGalleryIntent.ConfirmPhoto -> {
                postSideEffect(SystemGallerySideEffect.NavigateToBack(imageUri = intent.imageUri))
            }
        }
    }
}
