package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.segmentation.impl.repository.ImageSegmentationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SegmentationState(
    val originBitmap: Bitmap? = null,
    val overlayBitmap: Bitmap? = null,
    val subjectImagePath: String? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class LoadImage(val imageUri: String?) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect

@HiltViewModel
class SegmentationViewModel
@Inject constructor(
    private val repository: ImageSegmentationRepository,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.LoadImage -> {
                val uri = intent.imageUri ?: return

                viewModelScope.launch {
                    val bitmap = repository.decodeImage(uri)
                    updateState { copy(originBitmap = bitmap) }
                    val result = repository.segmentImage(bitmap)
                    updateState {
                        copy(
                            overlayBitmap = result.overlayBitmap,
                            subjectImagePath = result.subjectImagePath,
                        )
                    }
                }
            }
        }
    }
}
