package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
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
    val trimmedSubjectImagePath: String? = null,
    val subjectBounds: SegmentationBounds? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent

sealed interface SegmentationEffect : UiSideEffect

@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val clearSegmentationCacheUseCase: ClearSegmentationCacheUseCase,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val segmentImageUseCase: SegmentImageUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    init {
        viewModelScope.launch {
            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            // 지난 흐름의 파일을 못 지워도 이번 흐름은 진행돼야 한다 — 남은 파일은 다음 진입에서 다시 지운다
            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false, isError = true) }
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(sourceImageUri) }

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
                            trimmedSubjectImagePath = result.trimmedSubjectImagePath,
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
