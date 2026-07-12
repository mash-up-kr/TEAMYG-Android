package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
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

sealed interface SegmentationEffect : UiSideEffect {
    data object SegmentationFailed : SegmentationEffect
}

@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val segmentImageUseCase: SegmentImageUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    init {
        viewModelScope.launch {
            val bitmapWrapper = decodeImageUseCase(sourceImageUri)
            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { result ->
                    val overlayBitmap = (result.bitmap as? AndroidBitmap)?.getRawData()
                    updateState {
                        copy(
                            overlayBitmap = overlayBitmap,
                            subjectImagePath = result.subjectImagePath,
                        )
                    }
                }.onFailure { postSideEffect(SegmentationEffect.SegmentationFailed) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {}
}
