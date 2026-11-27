package com.teamyg.parfait.feature.groups.canvas.impl.solo

import android.graphics.Bitmap
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.toFirstDayOfMonth
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasToGalleryUseCase
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveParfaitDayBoundaryUseCase
import com.teamyg.parfait.domain.usecase.solo.DeleteSoloSpaceUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloCanvasFlowUseCase
import com.teamyg.parfait.domain.usecase.solo.GetSoloIntroPlayedDateUseCase
import com.teamyg.parfait.domain.usecase.solo.MarkSoloIntroPlayedUseCase
import com.teamyg.parfait.domain.usecase.solo.ResetSoloSpaceUseCase
import com.teamyg.parfait.domain.usecase.topping.StartToppingDraftUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase
import com.teamyg.parfait.feature.groups.canvas.impl.model.SoloDialog
import com.teamyg.parfait.feature.groups.canvas.impl.util.isEmptyForIntro
import com.teamyg.parfait.feature.groups.canvas.impl.util.nextToppingPositionZ
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import java.io.File

/**
 * @param canvas 그룹 캔버스 화면을 그대로 쓰려고 그 UiState 에 싣는다. 혼자 공간에서는 늘 오늘을 본다
 * @param isSoloActionRunning 비우기·삭제가 도는 동안. 그 사이에는 메뉴를 다시 열지 않는다
 */
data class SoloCanvasUiState(
    val canvas: CanvasMainUiState = CanvasMainUiState(),
    val soloDialog: SoloDialog? = null,
    val isSoloActionRunning: Boolean = false,
) : UiState

sealed interface SoloCanvasIntent : UiIntent {
    data object ClickMenu : SoloCanvasIntent

    data object ClickResetMenu : SoloCanvasIntent

    data object ClickDeleteMenu : SoloCanvasIntent

    /** 메뉴 바깥(딤)·뒤로가기·"그만두기" */
    data object DismissDialog : SoloCanvasIntent

    data object ConfirmReset : SoloCanvasIntent

    data object ConfirmDelete : SoloCanvasIntent

    data object ClickCamera : SoloCanvasIntent

    data object ClickGallery : SoloCanvasIntent

    data object ClickText : SoloCanvasIntent

    data object ClickSaveToGallery : SoloCanvasIntent

    /** 미리보기에서 저장을 확정하고 돌아왔다. 미리 보여 준 그 이미지를 화면이 다시 읽어 넘긴다 */
    data class SaveCapturedCanvas(val bitmap: Bitmap) : SoloCanvasIntent

    /** 저장 미리보기가 녹화를 마쳐 캐시에 mp4 를 완성하고 돌아왔다 */
    data class SaveRecordedVideo(val videoFilePath: String) : SoloCanvasIntent

    /** 쓰기 권한이 거부돼 만들어 온 mp4 를 저장하지 않게 됐다 */
    data class DiscardRecordedVideo(val videoFilePath: String) : SoloCanvasIntent

    /** 화면이 다시 보이기 시작했다(백그라운드 복귀 포함). 재생 중인 빈 캔버스 연출을 정리한다 */
    data object AppStarted : SoloCanvasIntent

    data object EmptyIntroAnimationFinished : SoloCanvasIntent

    data object EmptyIntroTap : SoloCanvasIntent

    /** 메뉴·뒤로가기·토핑 추가처럼 연출 바깥의 조작 */
    data object EmptyIntroInteraction : SoloCanvasIntent

    data object EmptyIntroDismissFinished : SoloCanvasIntent
}

sealed interface SoloCanvasEffect : UiSideEffect {
    data object NavigateToCamera : SoloCanvasEffect

    data object NavigateToGallery : SoloCanvasEffect

    data object NavigateToTextInput : SoloCanvasEffect

    /** 캡처와 캐시 파일 쓰기는 화면만 할 수 있어 요청만 보낸다 */
    data object RequestCanvasCaptureForPreview : SoloCanvasEffect

    data class ShowGallerySaveResult(
        val isSuccess: Boolean,
        val date: LocalDate,
    ) : SoloCanvasEffect

    data object ShowCanvasLoadError : SoloCanvasEffect

    data object ShowToppingFlowStartError : SoloCanvasEffect

    data object ShowResetFailed : SoloCanvasEffect

    data object ShowDeleteFailed : SoloCanvasEffect

    /** 삭제가 끝났다. 들어온 화면(로그인·설정)으로 돌아간다 */
    data object Close : SoloCanvasEffect
}

/**
 * 혼자 공간 캔버스. 기기 안에만 저장하므로 서버에 닿는 use case 와 애널리틱스를 들이지 않는다
 * (스펙 「모듈과 패키지」).
 */
@HiltViewModel(assistedFactory = SoloCanvasViewModel.Factory::class)
class SoloCanvasViewModel
@AssistedInject
constructor(
    /** "내 공간". 문자열 리소스라 화면이 읽어 넘긴다 */
    @Assisted private val title: String,
    private val getSoloCanvasFlow: GetSoloCanvasFlowUseCase,
    private val getSoloIntroPlayedDate: GetSoloIntroPlayedDateUseCase,
    private val markSoloIntroPlayed: MarkSoloIntroPlayedUseCase,
    private val resetSoloSpace: ResetSoloSpaceUseCase,
    private val deleteSoloSpace: DeleteSoloSpaceUseCase,
    private val startToppingDraft: StartToppingDraftUseCase,
    private val observeParfaitDayBoundary: ObserveParfaitDayBoundaryUseCase,
    private val saveCanvasToGallery: SaveCanvasToGalleryUseCase,
    private val saveCanvasVideoToGallery: SaveCanvasVideoToGalleryUseCase,
) : BaseViewModel<SoloCanvasUiState, SoloCanvasIntent, SoloCanvasEffect>(
    initialState = SoloCanvasUiState(canvas = CanvasMainUiState(groupName = title)),
) {
    init {
        viewModelLogger.i { "SoloCanvasViewModel::init" }
        observeSoloCanvas()
        observeDayBoundary()
    }

    /**
     * 읽기에 실패하면 받아 둔 캔버스를 그대로 두고 알리기만 한다. 손상된 저장본은 "비우기"로
     * 되살린다(스펙 「혼자 공간 캔버스」 결과).
     */
    private fun observeSoloCanvas() {
        launchWhileSubscribed(
            source = {
                getSoloCanvasFlow().onStart {
                    if (state.value.canvas.todayCanvas == null) updateCanvas { copy(isInitialLoading = true) }
                }
            },
        ) { result ->
            result
                .onSuccess { soloCanvas -> showCanvas(soloCanvas) }
                .onFailure { throwable ->
                    viewModelLogger.e(throwable) { "혼자 공간 캔버스를 읽지 못했다" }
                    updateCanvas { copy(isInitialLoading = false) }
                    postSideEffect(SoloCanvasEffect.ShowCanvasLoadError)
                }
        }
    }

    private fun showCanvas(soloCanvas: SoloCanvasVO) {
        val canvas = soloCanvas.toCanvasVO(today = state.value.canvas.today, title = title)
        updateCanvas { copy(todayCanvas = canvas, isInitialLoading = false) }
        syncEmptyIntro(canvas)
    }

    /**
     * 빈 캔버스 안내 연출. 그룹 캔버스(`CanvasMainViewModel.syncEmptyIntro`)와 같은 규칙이다 — 날짜마다 첫
     * 방출에서 한 번 정하고, 같은 날짜 안에서 캔버스가 중간에 비어도 뒤집지 않고, 시작하는 순간 기록한다.
     * 기록만 계정과 무관한 혼자 공간 전용이다.
     */
    private fun syncEmptyIntro(canvas: CanvasVO) {
        val isEmpty = canvas.isEmptyForIntro()

        if (state.value.canvas.emptyIntroCanvasDate != canvas.date) {
            updateCanvas {
                copy(
                    emptyIntroCanvasDate = canvas.date,
                    emptyIntroPhase = if (isEmpty) CanvasEmptyIntroPhase.Idle else CanvasEmptyIntroPhase.Dismissed,
                )
            }
            // 키를 날짜별로 둔다 — 앞 날짜 판정이 기록을 읽는 사이 날짜가 바뀌어도 새 날짜 판정이 버려지지 않는다.
            // 앞 날짜 작업은 startEmptyIntroIfUnplayed 의 날짜 가드에서 끝난다
            if (isEmpty) launch(key = EMPTY_INTRO_KEY to canvas.date) { startEmptyIntroIfUnplayed(canvas.date) }
        } else if (isEmpty.not()) {
            updateCanvas {
                when (emptyIntroPhase) {
                    CanvasEmptyIntroPhase.Entering, CanvasEmptyIntroPhase.Shown ->
                        copy(emptyIntroPhase = CanvasEmptyIntroPhase.Dismissed)

                    else -> this
                }
            }
        }
    }

    private suspend fun startEmptyIntroIfUnplayed(date: LocalDate) {
        val alreadyPlayed = getSoloIntroPlayedDate() == date

        // 기록을 읽는 사이 토핑이 올라왔거나 날짜·단계가 바뀌었으면 시작하지 않는다
        val current = state.value.canvas
        val isStillThisDecision = current.emptyIntroCanvasDate == date &&
            current.emptyIntroPhase == CanvasEmptyIntroPhase.Idle
        if (isStillThisDecision.not() || current.isEmptyPlaceholderVisible.not()) return

        if (alreadyPlayed) {
            updateCanvas { copy(emptyIntroPhase = CanvasEmptyIntroPhase.Dismissed) }
        } else {
            updateCanvas { copy(emptyIntroPhase = CanvasEmptyIntroPhase.Entering) }
            markSoloIntroPlayed(date)
        }
    }

    private fun updateEmptyIntroPhase(transform: (CanvasEmptyIntroPhase) -> CanvasEmptyIntroPhase) {
        updateCanvas { copy(emptyIntroPhase = transform(emptyIntroPhase)) }
    }

    /** 날짜 경계를 넘기면 날짜 헤더만 바뀐다. 혼자 공간은 날마다 새 캔버스를 열지 않는다 */
    private fun observeDayBoundary() {
        launchWhileSubscribed(source = { observeParfaitDayBoundary() }) { today ->
            if (today == state.value.canvas.today) return@launchWhileSubscribed

            updateCanvas {
                copy(
                    today = today,
                    selectedDate = today,
                    displayedMonth = today.toFirstDayOfMonth(),
                    todayCanvas = todayCanvas?.copy(date = today),
                )
            }
            // 날짜가 바뀌면 그 날짜의 연출 여부를 새로 정한다
            val canvas = state.value.canvas.todayCanvas ?: return@launchWhileSubscribed
            syncEmptyIntro(canvas)
        }
    }

    override fun processIntent(intent: SoloCanvasIntent) {
        when (intent) {
            SoloCanvasIntent.ClickMenu -> {
                if (state.value.isSoloActionRunning) return
                updateState { copy(soloDialog = SoloDialog.Menu) }
            }

            SoloCanvasIntent.ClickResetMenu -> updateState { copy(soloDialog = SoloDialog.ResetConfirm) }

            SoloCanvasIntent.ClickDeleteMenu -> updateState { copy(soloDialog = SoloDialog.DeleteConfirm) }

            SoloCanvasIntent.DismissDialog -> updateState { copy(soloDialog = null) }

            SoloCanvasIntent.ConfirmReset -> handleConfirmReset()

            SoloCanvasIntent.ConfirmDelete -> handleConfirmDelete()

            SoloCanvasIntent.ClickCamera -> startToppingFlow(SoloCanvasEffect.NavigateToCamera)

            SoloCanvasIntent.ClickGallery -> startToppingFlow(SoloCanvasEffect.NavigateToGallery)

            SoloCanvasIntent.ClickText -> startToppingFlow(SoloCanvasEffect.NavigateToTextInput)

            SoloCanvasIntent.ClickSaveToGallery -> postSideEffect(SoloCanvasEffect.RequestCanvasCaptureForPreview)

            is SoloCanvasIntent.SaveCapturedCanvas -> handleSaveCapturedCanvas(intent.bitmap)

            is SoloCanvasIntent.SaveRecordedVideo -> handleSaveRecordedVideo(intent.videoFilePath)

            is SoloCanvasIntent.DiscardRecordedVideo -> launch { deleteRecordedVideo(intent.videoFilePath) }

            SoloCanvasIntent.AppStarted -> updateEmptyIntroPhase(CanvasEmptyIntroPhase::onStop)

            SoloCanvasIntent.EmptyIntroAnimationFinished ->
                updateEmptyIntroPhase(CanvasEmptyIntroPhase::onAnimationFinished)

            SoloCanvasIntent.EmptyIntroTap -> updateEmptyIntroPhase(CanvasEmptyIntroPhase::onTap)

            SoloCanvasIntent.EmptyIntroInteraction -> updateEmptyIntroPhase(CanvasEmptyIntroPhase::onInteraction)

            SoloCanvasIntent.EmptyIntroDismissFinished ->
                updateEmptyIntroPhase(CanvasEmptyIntroPhase::onDismissFinished)
        }
    }

    /** 비우기 성공은 따로 알리지 않는다 — 저장소 흐름이 빈 캔버스를 다시 실어 온다 */
    private fun handleConfirmReset() {
        runSoloAction {
            resetSoloSpace().onFailure { throwable ->
                viewModelLogger.e(throwable) { "혼자 공간을 비우지 못했다" }
                postSideEffect(SoloCanvasEffect.ShowResetFailed)
            }
            updateState { copy(isSoloActionRunning = false) }
        }
    }

    /** 삭제에 성공하면 실행 중 표시를 내리지 않는다 — 화면이 닫히기 전에 메뉴를 다시 열지 못하게 */
    private fun handleConfirmDelete() {
        runSoloAction {
            deleteSoloSpace()
                .onSuccess { postSideEffect(SoloCanvasEffect.Close) }
                .onFailure { throwable ->
                    viewModelLogger.e(throwable) { "혼자 공간을 삭제하지 못했다" }
                    postSideEffect(SoloCanvasEffect.ShowDeleteFailed)
                    updateState { copy(isSoloActionRunning = false) }
                }
        }
    }

    /**
     * 팝업은 바로 닫는다. 실행 중 표시를 코루틴 밖에서 먼저 세워, 연타가 같은 프레임에 들어와도
     * 두 번째부터는 여기서 걸러진다.
     */
    private fun runSoloAction(block: suspend () -> Unit) {
        updateState { copy(soloDialog = null) }
        if (state.value.isSoloActionRunning) return

        updateState { copy(isSoloActionRunning = true) }
        launch(
            key = SOLO_ACTION_KEY,
            onError = { error ->
                viewModelLogger.e { "혼자 공간 작업이 예상 못 한 이유로 멈췄다 - $error" }
                updateState { copy(isSoloActionRunning = false) }
            },
        ) { block() }
    }

    /** 흐름에 들어서는 순간 초안을 새로 쓰고, 그 뒤에야 화면을 옮긴다(그룹 캔버스와 같다) */
    private fun startToppingFlow(effect: SoloCanvasEffect) {
        val canvas = state.value.canvas.todayCanvas ?: return

        launch(
            key = START_TOPPING_FLOW_KEY,
            onError = { error ->
                viewModelLogger.e { "토핑 초안을 쓰지 못했다 - $error" }
                postSideEffect(SoloCanvasEffect.ShowToppingFlowStartError)
            },
        ) {
            startToppingDraft(
                target = ToppingDraftTarget.Solo,
                nextPositionZ = nextToppingPositionZ(toppings = canvas.toppings, texts = canvas.texts),
            )
            postSideEffect(effect)
        }
    }

    private fun handleSaveCapturedCanvas(bitmap: Bitmap) {
        val date = state.value.canvas.today

        launch(key = SAVE_CANVAS_TO_GALLERY_KEY) {
            val displayName = "parfait_${System.currentTimeMillis()}.png"
            val result = saveCanvasToGallery(bitmap.toAndroidBitmap(), displayName)

            postSideEffect(SoloCanvasEffect.ShowGallerySaveResult(isSuccess = result.isSuccess, date = date))
        }
    }

    private fun handleSaveRecordedVideo(videoFilePath: String) {
        val date = state.value.canvas.today

        launch(key = SAVE_CANVAS_VIDEO_TO_GALLERY_KEY) {
            val displayName = "parfait_${System.currentTimeMillis()}.mp4"
            val result = saveCanvasVideoToGallery(videoFilePath = videoFilePath, displayName = displayName)

            // 다시 시도할 길이 없는 캐시 파일이라 결과와 상관없이 지운다
            deleteRecordedVideo(videoFilePath)

            postSideEffect(SoloCanvasEffect.ShowGallerySaveResult(isSuccess = result.isSuccess, date = date))
        }
    }

    private suspend fun deleteRecordedVideo(videoFilePath: String) {
        withContext(Dispatchers.IO) { File(videoFilePath).delete() }
    }

    private fun updateCanvas(reducer: CanvasMainUiState.() -> CanvasMainUiState) {
        updateState { copy(canvas = canvas.reducer()) }
    }

    @AssistedFactory
    interface Factory {
        fun create(title: String): SoloCanvasViewModel
    }

    private companion object {
        const val SOLO_ACTION_KEY = "soloAction"

        const val EMPTY_INTRO_KEY = "emptyIntro"

        const val START_TOPPING_FLOW_KEY = "startToppingFlow"

        const val SAVE_CANVAS_TO_GALLERY_KEY = "saveCanvasToGallery"

        const val SAVE_CANVAS_VIDEO_TO_GALLERY_KEY = "saveCanvasVideoToGallery"
    }
}
