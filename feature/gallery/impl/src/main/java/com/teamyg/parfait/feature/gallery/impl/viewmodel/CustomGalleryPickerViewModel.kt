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
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.gallery.LoadGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.topping.EnsureDraftSubjectRecordedUseCase
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Immutable
data class CustomGalleryPickerState(
    val isLoading: Boolean = false,
    val access: GalleryPermissionManager.GalleryAccessLevel = GalleryPermissionManager.GalleryAccessLevel.INITIAL,
    val groups: List<GalleryImageGroup> = emptyList(),
    val recentImages: List<RecentImage> = emptyList(),
) : UiState {
    val isEmpty: Boolean
        get() = groups.all { it.images.isEmpty() } && recentImages.isEmpty()
}

sealed class CustomGalleryPickerEffect private constructor() : UiSideEffect {
    data object RequestPermission : CustomGalleryPickerEffect()

    data object OpenAppSettings : CustomGalleryPickerEffect()

    data class NavigateToConfirm(
        val uri: String,
    ) : CustomGalleryPickerEffect()

    data object NavigateToToppingPlace : CustomGalleryPickerEffect()

    data object ShowDraftUnavailable : CustomGalleryPickerEffect()

    data object NavigateToBack : CustomGalleryPickerEffect()
}

sealed class CustomGalleryPickerIntent private constructor() : UiIntent {
    data class OnPermissionResult(
        val access: GalleryPermissionManager.GalleryAccessLevel,
    ) : CustomGalleryPickerIntent()

    data object OnRequestOpenSettings : CustomGalleryPickerIntent()

    data object OnRequestManageMedia : CustomGalleryPickerIntent()

    data class OnClickImage(
        val uri: String,
    ) : CustomGalleryPickerIntent()

    data class OnClickCutoutImage(
        val recentImage: RecentImage,
    ) : CustomGalleryPickerIntent()

    data object OnCancel : CustomGalleryPickerIntent()
}

@HiltViewModel(assistedFactory = CustomGalleryPickerViewModel.Factory::class)
class CustomGalleryPickerViewModel
@AssistedInject constructor(
    @Assisted private val returnResultOnly: Boolean,
    @Assisted private val recentImagePick: RecentImagePick,
    private val getRecentCacheImagesUseCase: GetRecentCacheImagesUseCase,
    private val loadGalleryImageGroupsUseCase: LoadGalleryImageGroupsUseCase,
    private val ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase,
) : BaseViewModel<CustomGalleryPickerState, CustomGalleryPickerIntent, CustomGalleryPickerEffect>(
    initialState = CustomGalleryPickerState(),
) {
    private var hasRequestedPermission = false
    private var loadGroupsJob: Job? = null

    init {
        viewModelLogger.i { "CustomGalleryPickerViewModel::init" }

        viewModelScope.launch {
            collectRecentCacheImages()
        }
    }

    override fun processIntent(intent: CustomGalleryPickerIntent) {
        when (intent) {
            is CustomGalleryPickerIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomGalleryPickerIntent.OnRequestOpenSettings -> handleOnRequestOpenSettings()
            is CustomGalleryPickerIntent.OnRequestManageMedia -> handleOnRequestManageMedia()
            is CustomGalleryPickerIntent.OnClickImage -> handleOnClickImage(intent)
            is CustomGalleryPickerIntent.OnClickCutoutImage -> handleOnClickCutoutImage(intent)
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

                // 권한 결과가 다시 오면 접근 수준이 달라졌을 수 있어 이전 조회를 버리고 새로 읽는다
                loadGroupsJob?.cancel()
                loadGroupsJob = viewModelScope.launch {
                    val result = runSuspendCatching { loadGalleryImageGroupsUseCase() }

                    result.fold(
                        onSuccess = { images ->
                            updateState {
                                copy(
                                    isLoading = false,
                                    groups = images,
                                )
                            }
                        },
                        onFailure = { e ->
                            viewModelLogger.e(e) { "갤러리 이미지 조회 실패" }

                            updateState { copy(isLoading = false) }
                        },
                    )
                }
            }

            false -> {
                updateState {
                    copy(
                        access = intent.access,
                    )
                }

                requestPermissionOnce()
            }
        }
    }

    private fun requestPermissionOnce() {
        if (hasRequestedPermission) return

        hasRequestedPermission = true
        postSideEffect(CustomGalleryPickerEffect.RequestPermission)
    }

    private fun handleOnRequestOpenSettings() {
        postSideEffect(CustomGalleryPickerEffect.OpenAppSettings)
    }

    private fun handleOnRequestManageMedia() {
        postSideEffect(CustomGalleryPickerEffect.RequestPermission)
    }

    private fun handleOnClickImage(intent: CustomGalleryPickerIntent.OnClickImage) {
        postSideEffect(CustomGalleryPickerEffect.NavigateToConfirm(intent.uri))
    }

    // 배치 화면은 초안을 읽기만 하므로 들어가기 전에 이 알맹이를 가리키게 맞춘다
    private fun handleOnClickCutoutImage(intent: CustomGalleryPickerIntent.OnClickCutoutImage) {
        launch(
            key = ENSURE_CUTOUT_DRAFT_KEY,
            onError = { postSideEffect(CustomGalleryPickerEffect.ShowDraftUnavailable) },
        ) {
            val isAligned = ensureDraftSubjectRecorded(intent.recentImage.filePath)

            postSideEffect(
                if (isAligned) {
                    CustomGalleryPickerEffect.NavigateToToppingPlace
                } else {
                    CustomGalleryPickerEffect.ShowDraftUnavailable
                },
            )
        }
    }

    private fun handleOnCancel() {
        postSideEffect(CustomGalleryPickerEffect.NavigateToBack)
    }

    private suspend fun collectRecentCacheImages() = getRecentCacheImagesUseCase().collect { images ->
        val wanted = when (recentImagePick) {
            RecentImagePick.SOURCE -> RecentImageKind.SOURCE
            RecentImagePick.CUTOUT -> RecentImageKind.CUTOUT
        }

        updateState { copy(recentImages = images.filter { it.kind == wanted }) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            returnResultOnly: Boolean,
            recentImagePick: RecentImagePick,
        ): CustomGalleryPickerViewModel
    }

    private companion object {
        const val ENSURE_CUTOUT_DRAFT_KEY = "ensureCutoutDraft"
    }
}
