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
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.member.CompleteTutorialUseCase
import com.teamyg.parfait.domain.usecase.member.GetTutorialVisibleFlowUseCase
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@Immutable
data class CustomGalleryPickerState(
    val isLoading: Boolean = false,
    val access: GalleryPermissionManager.GalleryAccessLevel = GalleryPermissionManager.GalleryAccessLevel.INITIAL,
    val groups: List<GalleryImageGroup> = emptyList(),
    val recentImages: List<RecentImage> = emptyList(),
    /** 앱 설치 후 이 화면 첫 진입에서만 `true`. 화면 전체를 덮는다 */
    val isTutorialVisible: Boolean = false,
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

    data class NavigateToSegmentationConfirm(
        val trimmedSubjectImagePath: String,
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

    data class OnClickCutoutImage(
        val recentImage: RecentImage,
    ) : CustomGalleryPickerIntent()

    data object OnCancel : CustomGalleryPickerIntent()

    /** 튜토리얼 칩을 눌렀다. 한 장뿐이라 그대로 닫고 다시 뜨지 않게 남긴다 */
    data object OnConfirmTutorial : CustomGalleryPickerIntent()
}

@HiltViewModel(assistedFactory = CustomGalleryPickerViewModel.Factory::class)
class CustomGalleryPickerViewModel
@AssistedInject constructor(
    @Assisted private val returnResultOnly: Boolean,
    @Assisted private val recentImagePick: RecentImagePick,
    private val getRecentCacheImagesUseCase: GetRecentCacheImagesUseCase,
    private val loadFilterYGGalleryImageGroupsUseCase: LoadFilterYGGalleryImageGroupsUseCase,
    private val getTutorialVisibleFlowUseCase: GetTutorialVisibleFlowUseCase,
    private val completeTutorialUseCase: CompleteTutorialUseCase,
) : BaseViewModel<CustomGalleryPickerState, CustomGalleryPickerIntent, CustomGalleryPickerEffect>(
    initialState = CustomGalleryPickerState(),
) {
    init {
        viewModelLogger.i { "CustomGalleryPickerViewModel::init" }

        viewModelScope.launch {
            collectRecentCacheImages()
        }

        observeTutorial()
    }

    private fun observeTutorial() {
        launchWhileSubscribed(source = { getTutorialVisibleFlowUseCase(TutorialKind.UPLOAD) }) { isVisible ->
            updateState { copy(isTutorialVisible = isVisible) }
        }
    }

    private fun handleConfirmTutorial() {
        updateState { copy(isTutorialVisible = false) }
        launch(key = COMPLETE_TUTORIAL_KEY) { completeTutorialUseCase(TutorialKind.UPLOAD) }
    }

    override fun processIntent(intent: CustomGalleryPickerIntent) {
        when (intent) {
            is CustomGalleryPickerIntent.OnPermissionResult -> handleOnPermissionResult(intent)
            is CustomGalleryPickerIntent.OnRequestPermission -> handleOnRequestPermission()
            is CustomGalleryPickerIntent.OnRequestOpenSettings -> handleOnRequestOpenSettings()
            is CustomGalleryPickerIntent.OnRequestManageMedia -> handleOnRequestManageMedia()
            is CustomGalleryPickerIntent.OnClickImage -> handleOnClickImage(intent)
            is CustomGalleryPickerIntent.OnClickCutoutImage -> handleOnClickCutoutImage(intent)
            is CustomGalleryPickerIntent.OnCancel -> handleOnCancel()
            is CustomGalleryPickerIntent.OnConfirmTutorial -> handleConfirmTutorial()
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
        postSideEffect(CustomGalleryPickerEffect.NavigateToConfirm(intent.uri))
    }

    // 이미 누끼가 끝난 알맹이라 카메라·세그멘테이션을 건너뛴다
    private fun handleOnClickCutoutImage(intent: CustomGalleryPickerIntent.OnClickCutoutImage) {
        postSideEffect(
            CustomGalleryPickerEffect.NavigateToSegmentationConfirm(intent.recentImage.filePath),
        )
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
        const val COMPLETE_TUTORIAL_KEY = "completeTutorial"
    }
}
