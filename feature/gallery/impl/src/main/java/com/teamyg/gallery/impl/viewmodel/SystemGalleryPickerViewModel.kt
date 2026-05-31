package com.teamyg.gallery.impl.viewmodel

import android.net.Uri
import com.tjyg.core.ui.BaseViewModel
import com.tjyg.core.ui.UiIntent
import com.tjyg.core.ui.UiSideEffect
import com.tjyg.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class SystemGalleryState(
    val imageUri: Uri?,
) : UiState

sealed interface SystemGalleryIntent : UiIntent

sealed interface SystemGallerySideEffect : UiSideEffect

@HiltViewModel
class SystemGalleryPickerViewModel
    @Inject
    constructor() :
    BaseViewModel<SystemGalleryState, SystemGalleryIntent, SystemGallerySideEffect>(
        initialState = SystemGalleryState(
            imageUri = null,
        ),
    ) {
        override fun processIntent(intent: SystemGalleryIntent) {

        }
    }
