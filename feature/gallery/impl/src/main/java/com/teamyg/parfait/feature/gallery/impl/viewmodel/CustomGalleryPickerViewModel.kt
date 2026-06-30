package com.teamyg.parfait.feature.gallery.impl.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager
import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CustomGalleryPickerState(
    val isLoading: Boolean = false,
    val access: GalleryPermissionManager.GalleryAccessLevel = GalleryPermissionManager.GalleryAccessLevel.INITIAL,
    val groups: List<GalleryImageGroup> = emptyList(),
    val recentImages: List<String> = emptyList(),
) : UiState {
    val isEmpty: Boolean
        get() = groups.all { it.images.isEmpty() } && recentImages.isEmpty()
}

sealed class CustomGalleryPickerEffect private constructor() : UiSideEffect {
    data object RequestPermission : CustomGalleryPickerEffect()

    data object OpenAppSettings : CustomGalleryPickerEffect()

    data class ReturnResult(
        val uri: String,
    ) : CustomGalleryPickerEffect()

    data object NavigateToBack : CustomGalleryPickerEffect()
}

sealed class CustomGalleryPickerIntent private constructor() : UiIntent {
    data class OnPermissionResult(
        val access: GalleryPermissionManager.GalleryAccessLevel,
    ) : CustomGalleryPickerIntent()

    data object OnRequestPermission : CustomGalleryPickerIntent()

    data object OnRequestOpenSettings : CustomGalleryPickerIntent()

    data object OnRequestManageMedia : CustomGalleryPickerIntent()

    data class OnClickImage(
        val uri: String,
    ) : CustomGalleryPickerIntent()

    data object OnCancel : CustomGalleryPickerIntent()
}

@HiltViewModel
class CustomGalleryPickerViewModel
@Inject
constructor(
    private val getRecentCacheImagesUseCase: GetRecentCacheImagesUseCase,
    private val loadFilterYGGalleryImageGroupsUseCase: LoadFilterYGGalleryImageGroupsUseCase,
) : BaseViewModel<CustomGalleryPickerState, CustomGalleryPickerIntent, CustomGalleryPickerEffect>(
    initialState = CustomGalleryPickerState(),
) {
    init {
        viewModelLogger.i { "CustomGalleryPickerViewModel::init" }

        viewModelScope.launch {
            collectRecentCacheImages()
        }
    }

    override fun processIntent(intent: CustomGalleryPickerIntent) {
        when (intent) {
            is CustomGalleryPickerIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomGalleryPickerIntent.OnRequestPermission -> handleOnRequestPermission()
            is CustomGalleryPickerIntent.OnRequestOpenSettings -> handleOnRequestOpenSettings()
            is CustomGalleryPickerIntent.OnRequestManageMedia -> handleOnRequestManageMedia()
            is CustomGalleryPickerIntent.OnClickImage -> handleOnClickImage(intent)
            is CustomGalleryPickerIntent.OnCancel -> handleOnCancel()
        }
    }

    private fun handleOnPermissionResult(intent: CustomGalleryPickerIntent.OnPermissionResult) {
        when (intent.access.hasPermission) {
            true -> {
                updateState {
                    copy(
                        isLoading = true,
                        access = intent.access,
                    )
                }

                viewModelScope.launch {
                    val images = loadFilterYGGalleryImageGroupsUseCase()

                    updateState {
                        copy(
                            isLoading = false,
                            groups = images,
                        )
                    }
                }
            }

            false -> {
                updateState {
                    copy(
                        access = intent.access,
                    )
                }
            }
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

    private fun handleOnClickImage(intent: CustomGalleryPickerIntent.OnClickImage) {
        postSideEffect(CustomGalleryPickerEffect.ReturnResult(intent.uri))
    }

    private fun handleOnCancel() {
        postSideEffect(CustomGalleryPickerEffect.NavigateToBack)
    }

    private suspend fun collectRecentCacheImages() = getRecentCacheImagesUseCase().collect { uris ->
        updateState { copy(recentImages = uris) }
    }
}
