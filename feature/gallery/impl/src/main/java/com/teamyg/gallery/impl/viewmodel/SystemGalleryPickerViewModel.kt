package com.teamyg.gallery.impl.viewmodel

import com.teamyg.analytics.AnalyticsHelper
import com.teamyg.model.qualifier.ViewModelQualifier
import com.tjyg.core.ui.base.BaseViewModel
import com.tjyg.core.ui.mvi.UiIntent
import com.tjyg.core.ui.mvi.UiSideEffect
import com.tjyg.core.ui.mvi.UiState
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
    constructor(
        @ViewModelQualifier analyticsHelper: AnalyticsHelper,
    ) :
    BaseViewModel<SystemGalleryState, SystemGalleryIntent, SystemGallerySideEffect>(
            analyticsHelper = analyticsHelper,
            initialState = SystemGalleryState(),
        ) {
        init {
            analyticsHelper.d { "SystemGalleryPickerViewModel::init" }
        }

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
