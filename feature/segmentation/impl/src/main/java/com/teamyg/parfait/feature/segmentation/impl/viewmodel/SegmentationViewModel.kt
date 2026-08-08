package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

data class SegmentationState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val originBitmap: Bitmap? = null,
    val subjectImagePath: String? = null,
    val subjectBounds: SegmentationBounds? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent

sealed interface SegmentationEffect : UiSideEffect

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
                    val subjectBounds = result.subjectBounds

                    // bounds 가 없으면 하이라이트도 다음 화면으로 갈 방법도 없는 화면만 남는다
                    if (subjectBounds == null) {
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    updateState {
                        copy(
                            subjectImagePath = result.subjectImagePath,
                            subjectBounds = subjectBounds,
                        )
                    }
                }.onFailure { updateState { copy(isError = true) } }

            // 실패해도 로딩 화면에 갇히지 않도록 성공/실패와 무관하게 해제한다
            updateState { copy(isLoading = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {}
}
