package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase
import com.teamyg.parfait.domain.usecase.image.RecoverCandidatesUseCase
import com.teamyg.parfait.domain.usecase.image.SaveBitmapUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 참이면 화면 전체가 `C-103-Error` 로 바뀐다 */
    val isError: Boolean = false,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent

    data object Retry : SegmentationIntent

    data object EditManually : SegmentationIntent

    data class OnEditResult(val result: ToppingEditResult) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    /** 화면을 덮지 않고 토스트로만 알리는 실패. 화면 전체를 바꾸는 실패는 [SegmentationState.isError] 가 받는다 */
    data object ShowError : SegmentationEffect

    /**
     * 원본을 못 읽었다. 이 경로를 실패 화면으로 돌리면 「직접 편집」이 쓸 원본이 없는 상태가
     * 생겨 버튼에 비활성 분기가 필요해진다.
     */
    data object GoBack : SegmentationEffect

    data class GoToEdit(val originImagePath: String) : SegmentationEffect

    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
}

@HiltViewModel(assistedFactory = SegmentationViewModel.Factory::class)
class SegmentationViewModel
@AssistedInject constructor(
    @Assisted private val sourceImageUri: String,
    private val addRecentImageUseCase: AddRecentImageUseCase,
    private val clearSegmentationCacheUseCase: ClearSegmentationCacheUseCase,
    private val decodeImageUseCase: DecodeImageUseCase,
    private val segmentImageUseCase: SegmentImageUseCase,
    private val recoverCandidatesUseCase: RecoverCandidatesUseCase,
    private val persistSubjectUseCase: PersistSubjectUseCase,
    private val saveBitmapUseCase: SaveBitmapUseCase,
    private val recordToppingDraft: RecordToppingDraftUseCase,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    /** `AndroidBitmap` 생성자가 모듈 내부라 `state.originBitmap` 으로는 다시 만들 수 없다 */
    private var originBitmapWrapper: BitmapWrapper? = null

    private enum class LastFailure { EXCEPTION, EMPTY }

    /** 재시도가 무엇을 돌지만 정한다. 화면이 안 쓰는 값이라 상태로 올리지 않는다 */
    private var lastFailure: LastFailure? = null

    /**
     * 이 사진으로 사다리를 끝까지 돌렸는가. 입력이 같으면 결과도 같아서 한 번만 돈다.
     *
     * ⚠️ 1차에서 다시 0건이 나와도 되돌리지 않는다. 되돌리면 사다리가 한 번 걸러 되풀이된다.
     */
    private var recoveryAttempted = false

    init {
        loadCandidates()
    }

    /**
     * ⚠️ 진입과 재시도가 **같은 키**를 쓴다. 진입만 다른 경로로 띄우면 진입 흐름이 도는 중에
     * 누른 재시도를 막지 못한다.
     */
    private fun loadCandidates() {
        launch(key = LOAD_CANDIDATES_KEY) {
            // 실패 표시를 걷지 않으면 재시도가 성공해도 에러 화면이 그대로 남는다
            updateState { copy(isLoading = true, isError = false, candidates = emptyList()) }

            // 이번 흐름이 파일을 만들기 전에 지운다 — 뒤에 두면 방금 만든 것을 지운다
            // 지난 흐름의 파일을 못 지워도 이번 흐름은 진행돼야 한다 — 남은 파일은 다음 진입에서 다시 지운다
            runSuspendCatching { clearSegmentationCacheUseCase() }

            val bitmapWrapper = decodeImageUseCase(sourceImageUri).getOrNull()

            if (bitmapWrapper == null) {
                updateState { copy(isLoading = false) }
                postSideEffect(SegmentationEffect.GoBack)
                return@launch
            }

            // 디코드를 통과한 뒤에 기록한다 — 열리지 않는 이미지를 남기면 최근 목록의 자리만 차지한다
            runSuspendCatching { addRecentImageUseCase(source = sourceImageUri, kind = RecentImageKind.SOURCE) }

            val originBitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            originBitmapWrapper = bitmapWrapper
            updateState { copy(originBitmap = originBitmap) }

            segmentImageUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    if (candidates.isEmpty()) {
                        lastFailure = LastFailure.EMPTY
                        updateState { copy(isError = true) }
                        return@onSuccess
                    }

                    lastFailure = null
                    updateState { copy(candidates = candidates) }
                }.onFailure { throwable ->
                    // 화면이 원인을 가르지 않으므로 원인은 여기에만 남는다
                    viewModelLogger.e(throwable) {
                        "세그멘테이션 실패 ${throwable::class.simpleName}, 원인 ${throwable.cause}"
                    }
                    lastFailure = LastFailure.EXCEPTION
                    updateState { copy(isError = true) }
                }

            // 실패해도 로딩 오버레이에 갇히지 않도록 성공/실패와 무관하게 해제한다
            updateState { copy(isLoading = false) }
        }
    }

    /**
     * ⚠️ [LOAD_CANDIDATES_KEY] 를 진입·재시도와 공유한다. 다른 키를 쓰면 연타가 사다리를 겹쳐 돈다.
     */
    private fun recover() {
        val bitmapWrapper = originBitmapWrapper ?: return loadCandidates()

        launch(
            key = LOAD_CANDIDATES_KEY,
            onError = {
                lastFailure = LastFailure.EXCEPTION
                updateState { copy(isLoading = false, isError = true) }
            },
        ) {
            // 에러 표시를 안 걷으면 실패 화면 위에 로딩 덮개가 겹친다
            updateState { copy(isLoading = true, isError = false, candidates = emptyList()) }

            recoverCandidatesUseCase(bitmapWrapper)
                .onSuccess { candidates ->
                    // 끝까지 돌았다. 모듈 문제로 중간에 접히면 여기 오지 않아 다시 돌 기회가 남는다
                    recoveryAttempted = true
                    if (candidates.isEmpty()) {
                        lastFailure = LastFailure.EMPTY
                        updateState { copy(isError = true) }
                    } else {
                        lastFailure = null
                        updateState { copy(candidates = candidates) }
                    }
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "회복 실패 ${throwable::class.simpleName}" }
                    lastFailure = LastFailure.EXCEPTION
                    updateState { copy(isError = true) }
                }

            updateState { copy(isLoading = false) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(sourceImageUri: String): SegmentationViewModel
    }

    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.ClickCandidate -> selectCandidate(intent.index)

            SegmentationIntent.Retry ->
                if (lastFailure == LastFailure.EMPTY && !recoveryAttempted) recover() else loadCandidates()

            SegmentationIntent.EditManually -> editManually()

            is SegmentationIntent.OnEditResult -> recordEditResult(intent.result)
        }
    }

    /**
     * 저장 → 초안 기록 → 이동 순서를 지킨다. 그 순서인 이유는
     * `parfait/specs/2026-08-23-c103-multi-subject-selection.md`의 「선택 시점에 일어나는
     * 일의 순서」절.
     */
    private fun selectCandidate(index: Int) {
        val candidate = state.value.candidates.getOrNull(index) ?: return

        launch(
            key = SELECT_CANDIDATE_KEY,
            onError = {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
            },
        ) {
            updateState { copy(isLoading = true) }

            persistSubjectUseCase(candidate)
                .onSuccess { result ->
                    val recorded = runSuspendCatching {
                        recordToppingDraft(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
                            sourceLongSide = result.sourceLongSide,
                        )
                    }.getOrDefault(false)

                    // 이동이 goTo 라 이 화면이 백스택에 남는다. 켠 채 나가면 돌아왔을 때 갇힌다
                    releaseLoading()

                    if (recorded) {
                        postSideEffect(
                            SegmentationEffect.GoToConfirm(
                                subjectImagePath = result.subjectImagePath,
                                trimmedSubjectImagePath = result.trimmedSubjectImagePath,
                            ),
                        )
                    } else {
                        postSideEffect(SegmentationEffect.ShowError)
                    }
                }.onFailure {
                    releaseLoading()
                    postSideEffect(SegmentationEffect.ShowError)
                }
        }
    }

    /** 초안은 여기서 적지 않고 편집 결과를 받은 뒤에 적는다 */
    private fun editManually() {
        val originBitmapWrapper = originBitmapWrapper ?: return

        launch(
            key = EDIT_MANUALLY_KEY,
            onError = {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
            },
        ) {
            updateState { copy(isLoading = true) }

            val path = saveBitmapUseCase(originBitmapWrapper).getOrElse {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
                return@launch
            }

            releaseLoading()
            postSideEffect(SegmentationEffect.GoToEdit(originImagePath = path))
        }
    }

    private fun recordEditResult(result: ToppingEditResult) {
        launch(
            key = EDIT_RESULT_KEY,
            onError = {
                releaseLoading()
                postSideEffect(SegmentationEffect.ShowError)
            },
        ) {
            updateState { copy(isLoading = true) }

            val recorded = recordToppingDraft.recordEditResult(result)

            releaseLoading()

            if (recorded) {
                postSideEffect(
                    // 편집 결과와 확인 화면 키는 경로 이름이 서로 반대다(`ToppingEditResult` KDoc)
                    SegmentationEffect.GoToConfirm(
                        subjectImagePath = result.cutoutImagePath,
                        trimmedSubjectImagePath = result.subjectImagePath,
                    ),
                )
            } else {
                postSideEffect(SegmentationEffect.ShowError)
            }
        }
    }

    private fun releaseLoading() {
        updateState { copy(isLoading = false) }
    }
}

private const val SELECT_CANDIDATE_KEY = "select-candidate"
private const val LOAD_CANDIDATES_KEY = "loadCandidates"
private const val EDIT_MANUALLY_KEY = "edit-manually"
private const val EDIT_RESULT_KEY = "edit-result"
