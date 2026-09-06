package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.member.CompleteTutorialUseCase
import com.teamyg.parfait.domain.usecase.member.GetTutorialVisibleFlowUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"
private const val CUTOUT_PATH = "/cache/segmentation/subject.png"
private const val REUSED_PATH = "/data/files/recent_images/b.png"
private const val EDITED_CUTOUT_PATH = "/cache/segmentation/edited-cutout.png"

class SegmentationConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toppingDraftRepository: ToppingDraftRepository = mockk()
    private val getTutorialVisible: GetTutorialVisibleFlowUseCase = mockk()
    private val completeTutorial: CompleteTutorialUseCase = mockk(relaxed = true)

    /** 기본값은 "이미 본 사용자" — 튜토리얼이 다른 테스트의 화면 상태에 끼어들지 않게 한다 */
    private val tutorialVisible = MutableStateFlow(false)

    init {
        every { getTutorialVisible(TutorialKind.SEGMENTATION) } returns tutorialVisible
    }

    private fun givenDraft(draft: ToppingDraft?) {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
    }

    private fun draft(
        subjectImagePath: String? = SUBJECT_PATH,
        cutoutImagePath: String? = CUTOUT_PATH,
        borderColorArgb: Int? = null,
        borderWidthDp: Float? = null,
    ) = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 3,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )

    private fun viewModel() = SegmentationConfirmViewModel(
        subjectImagePath = SUBJECT_PATH,
        cutoutImagePath = CUTOUT_PATH,
        sourceImageUri = "content://media/1",
        savedStateHandle = SavedStateHandle(),
        toppingDraftRepository = toppingDraftRepository,
        getTutorialVisibleFlowUseCase = getTutorialVisible,
        completeTutorialUseCase = completeTutorial,
    )

    // 복원을 모사하는 테스트는 같은 handle 을 두 ViewModel 에 물린다
    private fun reuseViewModel(handle: SavedStateHandle = SavedStateHandle()) = SegmentationConfirmViewModel(
        subjectImagePath = REUSED_PATH,
        cutoutImagePath = null,
        sourceImageUri = null,
        savedStateHandle = handle,
        toppingDraftRepository = toppingDraftRepository,
        getTutorialVisibleFlowUseCase = getTutorialVisible,
        completeTutorialUseCase = completeTutorial,
    )

    /**
     * 튜토리얼 구독은 `launchWhileSubscribed` 라 [SegmentationConfirmViewModel.state] 를 보는
     * 쪽이 있어야 열린다 — 라우트의 `collectAsStateWithLifecycle()` 을 여기서 흉내 낸다.
     */
    private fun TestScope.shownViewModel() = viewModel().also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    @Test
    fun tutorial_onFirstEntry_coversTheScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 누끼 튜토리얼을 보지 않은 사용자
        givenDraft(draft())
        tutorialVisible.value = true

        // When 화면이 열린다
        val viewModel = shownViewModel()

        // Then 튜토리얼이 뜬다
        assertTrue(viewModel.state.value.isTutorialVisible)
    }

    @Test
    fun tutorial_alreadySeen_doesNotOpen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 본 사용자(기본 스텁)
        givenDraft(draft())

        // When 화면이 열린다
        val viewModel = shownViewModel()

        // Then 아무것도 덮지 않는다
        assertFalse(viewModel.state.value.isTutorialVisible)
    }

    @Test
    fun tutorial_onConfirm_closesItAndMarksItSeen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 튜토리얼이 떠 있다
        givenDraft(draft())
        tutorialVisible.value = true
        val viewModel = shownViewModel()

        // When 칩을 누른다
        viewModel.processIntent(SegmentationConfirmIntent.OnConfirmTutorial)
        advanceUntilIdle()

        // Then 닫히고, 다음 진입부터 뜨지 않도록 저장한다
        assertFalse(viewModel.state.value.isTutorialVisible)
        coVerify(exactly = 1) { completeTutorial(TutorialKind.SEGMENTATION) }
    }

    @Test
    fun state_followsTheDraft_notTheEntryArguments() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집을 거쳐 테두리까지 적힌 초안
        givenDraft(draft(borderColorArgb = 0xFF00FF00.toInt(), borderWidthDp = 4f))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 겹치는 구간에서는 초안이 정본이다 — 편집을 거치면 진입 인자가 낡는다
        val state = viewModel.state.value
        assertEquals(0xFF00FF00.toInt(), state.borderColorArgb)
        assertEquals(4f, state.borderWidthDp)
        assertTrue(state.isDraftReady)
    }

    @Test
    fun onEnter_writesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 이미 채워져 있다
        givenDraft(draft())

        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 화면이 열렸다는 이유로 초안에 쓰지 않는다 — 프로세스 사망 복원에서 진입 인자가
        // 편집 결과를 덮어쓰는 경로가 그렇게 생긴다
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
    }

    @Test
    fun onEditResult_recordsBorderValues() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 열려 있다
        givenDraft(draft())
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집을 마치고 돌아온다
        viewModel.processIntent(
            SegmentationConfirmIntent.OnEditResult(
                ToppingEditResult(
                    subjectImagePath = "/cache/segmentation/edited.png",
                    cutoutImagePath = "/cache/segmentation/edited-cutout.png",
                    borderLayers = listOf(ToppingBorderLayer(colorArgb = 0xFFFF0000.toInt(), widthDp = 8f)),
                ),
            ),
        )
        advanceUntilIdle()

        // Then 테두리는 굽지 않고 값으로 적힌다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(
                subjectImagePath = "/cache/segmentation/edited.png",
                cutoutImagePath = "/cache/segmentation/edited-cutout.png",
                borderColorArgb = 0xFFFF0000.toInt(),
                borderWidthDp = 8f,
            )
        }
    }

    @Test
    fun draft_carriesTheBorder_backIntoTheEditor() = runTest(mainDispatcherRule.dispatcher) {
        // Given 한 번 두른 테두리가 초안에 있다
        givenDraft(draft(borderColorArgb = 0xFF0000FF.toInt(), borderWidthDp = 6f))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 다시 편집을 열 때 벗겨진 채로 열리지 않는다
        assertEquals(
            listOf(ToppingBorderLayer(colorArgb = 0xFF0000FF.toInt(), widthDp = 6f)),
            viewModel.state.value.borderLayers,
        )
    }

    @Test
    fun draft_withoutSubject_blocksNext_andTellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 가리키던 캐시 파일이 사라졌거나 흐름이 열려 있지 않다
        givenDraft(draft(subjectImagePath = null))

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 알리고 다음으로 못 가게 막는다 — 여기서 안 막으면 배치 화면까지 가서야 올릴 데가
        // 없다는 것을 알게 된다
        viewModel.effect.test {
            advanceUntilIdle()
            assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
        }
        assertFalse(viewModel.state.value.isDraftReady)
    }

    @Test
    fun draft_turnsEmptyMidSession_blocksNext_keepsPaths_andReportsOncePerOccurrence() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 정상 초안이 흐르다 캐시 파일이 사라져 두 번 연달아 비고, 다시 정상으로
            // 돌아왔다가 또 빈다 — 화면이 떠 있는 동안 저장소가 이 순서로 재방출할 수 있다
            val normal = draft(borderColorArgb = 0xFF00FF00.toInt(), borderWidthDp = 4f)
            val empty = draft(subjectImagePath = null)
            every { toppingDraftRepository.draft } returns flowOf(normal, empty, empty, normal, empty)
            coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true

            // When 화면이 열린다
            val viewModel = viewModel()

            // Then 초안이 빌 때마다(연달아 두 번은 한 번으로 묶어) 알림이 오지만 쌓이지 않는다 —
            // 총 두 번(첫 공백 구간, 재개 뒤 다시 빈 구간)만 온다
            viewModel.effect.test {
                advanceUntilIdle()
                assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
                assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
            }

            // Then 마지막 빈 초안에서도 화면이 깜빡이지 않도록 경로 값은 남고 다음 버튼만 잠긴다
            val state = viewModel.state.value
            assertFalse(state.isDraftReady)
            assertEquals(SUBJECT_PATH, state.subjectImagePath)
        }

    @Test
    fun draft_throws_tellsTheUser_insteadOfDyingSilently() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안 흐름이 던진다(DataStore 읽기 실패 등)
        every { toppingDraftRepository.draft } returns flow { throw IllegalStateException("boom") }

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 수집이 조용히 죽지 않고, 이미 있는 이펙트로 사용자에게 알린다
        viewModel.effect.test {
            advanceUntilIdle()
            assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
        }
        assertFalse(viewModel.state.value.isDraftReady)
    }

    @Test
    fun reuseEntry_withEmptyDraft_recordsBeforeObserving() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스가 흐름은 열었지만 알맹이는 아직 없는 초안 — 최근 목록에서 고른 진입이다.
        // flowOf 는 곧장 완결돼 순서가 뒤집혀도 record() 가 결국 불려 테스트를 속인다 —
        // 끝나지 않는 MutableStateFlow 라야 순서 위반을 잡는다
        every { toppingDraftRepository.draft } returns MutableStateFlow(
            draft(subjectImagePath = null, cutoutImagePath = null),
        )
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true

        // When 화면이 열린다
        reuseViewModel()
        advanceUntilIdle()

        // Then 구독보다 먼저 적는다. 뒤집으면 첫 방출의 null 이 DraftMissing 토스트를 쏴
        // 사용자가 없는 실패를 듣는다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(REUSED_PATH, null, null, null)
        }
    }

    @Test
    fun reuseEntry_whenDraftAlreadyHasSubject_doesNotRecordAgain() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 이 알맹이가 적힌 초안 — 프로세스 사망 복원으로 돌아온 자리다
        givenDraft(draft(subjectImagePath = REUSED_PATH, cutoutImagePath = null, borderColorArgb = 0xFF00FF00.toInt()))

        // When 화면이 다시 열린다
        val viewModel = reuseViewModel()
        advanceUntilIdle()

        // Then 다시 적지 않는다 — record 는 테두리까지 통째로 덮어쓰므로 여기서 다시 적으면
        // 사용자가 방금 두른 테두리가 사라진다
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
        assertEquals(0xFF00FF00.toInt(), viewModel.state.value.borderColorArgb)
    }

    @Test
    fun reuseEntry_whenDraftHasDifferentSubject_recordsTheNewOne() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스 → 갤러리 → 다른 최근 알맹이를 이미 골라 적은 초안이 있다 — 뒤로가기로
        // 갤러리에 돌아가 이번에는 다른 알맹이를 고른 자리다
        givenDraft(
            draft(
                subjectImagePath = "/data/files/recent_images/a.png",
                cutoutImagePath = null,
                borderColorArgb = 0xFF00FF00.toInt(),
            ),
        )

        // When 새 알맹이로 화면이 다시 열린다
        reuseViewModel()
        advanceUntilIdle()

        // Then 초안이 가리키는 경로가 다르므로 새로 적는다 — "초안이 비어 있는가"로 판정하면
        // 이 경우를 놓쳐 옛 알맹이가 그대로 배치된다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(REUSED_PATH, null, null, null)
        }
    }

    @Test
    fun reuseEntry_afterBorderEdit_survivesProcessDeath_withoutOverwritingTheEdit() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 재사용 진입이 알맹이를 적은 뒤, 사용자가 테두리를 고쳐 초안이 편집 결과를 든다
            val draftFlow = MutableStateFlow(draft(subjectImagePath = null, cutoutImagePath = null))
            every { toppingDraftRepository.draft } returns draftFlow
            coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true

            val savedStateHandle = SavedStateHandle()
            reuseViewModel(savedStateHandle)
            advanceUntilIdle()
            draftFlow.value = draft(
                subjectImagePath = "/cache/segmentation/edited.png",
                cutoutImagePath = EDITED_CUTOUT_PATH,
                borderColorArgb = 0xFFFF0000.toInt(),
                borderWidthDp = 8f,
            )

            // When 프로세스가 죽었다 살아나 같은 진입 인자로 화면이 다시 열린다
            reuseViewModel(savedStateHandle)
            advanceUntilIdle()

            // Then 진입 인자로 초안을 덮어쓰지 않는다 — 덮으면 방금 두른 테두리와 편집 결과가
            // 말없이 사라진다. 적는 것은 첫 진입의 한 번뿐이다
            coVerify(exactly = 1) {
                toppingDraftRepository.record(REUSED_PATH, null, null, null)
            }
        }

    @Test
    fun normalEntry_opensAreaAndBorderEdit_fromTheReeditMask() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세그멘테이션을 거쳐 온 진입 — 원본도 재편집 마스크도 있다
        givenDraft(draft())

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 지운 영역을 되살릴 수 있으므로 영역까지 여는 편집으로 간다
        val state = viewModel.state.value
        assertFalse(state.isBorderOnlyEdit)
        assertEquals(CUTOUT_PATH, state.editImagePath)
    }

    @Test
    fun reuseEntry_opensBorderOnlyEdit_fromTheSubject() = runTest(mainDispatcherRule.dispatcher) {
        // Given 최근 목록에서 고른 진입 — 원본도 재편집 마스크도 없다
        givenDraft(draft(subjectImagePath = REUSED_PATH, cutoutImagePath = null))

        // When 화면이 열린다
        val viewModel = reuseViewModel()
        advanceUntilIdle()

        // Then 잠그지 않고 테두리만 고치는 편집으로 연다. 되살릴 원본이 없어 알맹이가 곧 재료다
        val state = viewModel.state.value
        assertTrue(state.isBorderOnlyEdit)
        assertEquals(REUSED_PATH, state.editImagePath)
    }

    @Test
    fun reuseEntry_afterBorderEdit_reopensFromTheEditedMask() = runTest(mainDispatcherRule.dispatcher) {
        // Given 재사용 진입에서 테두리를 한 번 고쳐 초안에 마스크가 적혔다
        givenDraft(draft(subjectImagePath = REUSED_PATH, cutoutImagePath = EDITED_CUTOUT_PATH))

        // When 화면이 열린다
        val viewModel = reuseViewModel()
        advanceUntilIdle()

        // Then 두 번째 편집은 그 마스크에서 이어간다. 원본은 여전히 없으므로 테두리 전용은 그대로다
        val state = viewModel.state.value
        assertTrue(state.isBorderOnlyEdit)
        assertEquals(EDITED_CUTOUT_PATH, state.editImagePath)
    }
}
