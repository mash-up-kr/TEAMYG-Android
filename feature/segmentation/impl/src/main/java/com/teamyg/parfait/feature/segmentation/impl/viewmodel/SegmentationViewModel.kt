package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.segmentation.impl.repository.ImageSegmentationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

data class SegmentationState(
    val originBitmap: Bitmap? = null,
    val overlayBitmap: Bitmap? = null,
    val subjectImagePath: String? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent

sealed interface SegmentationEffect : UiSideEffect

@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val repository: ImageSegmentationRepository,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    init {
        viewModelScope.launch {
            val bitmap = repository.decodeImage(sourceImageUri)
            updateState { copy(originBitmap = bitmap) }
            runCatching { repository.segmentImage(bitmap) }
                .onSuccess { result ->
                    updateState {
                        copy(
                            overlayBitmap = result.overlayBitmap,
                            subjectImagePath = result.subjectImagePath,
                        )
                    }
                }.onFailure {
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {}
}
