package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"
private const val CUTOUT_PATH = "/cache/segmentation/subject.png"

class SegmentationConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toppingDraftRepository: ToppingDraftRepository = mockk()

    private fun givenDraft(draft: ToppingDraft?) {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
    }

    private fun draft(
        subjectImagePath: String? = SUBJECT_PATH,
        borderColorArgb: Int? = null,
        borderWidthDp: Float? = null,
    ) = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 3,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = CUTOUT_PATH,
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )

    private fun viewModel() = SegmentationConfirmViewModel(
        subjectImagePath = SUBJECT_PATH,
        cutoutImagePath = CUTOUT_PATH,
        toppingDraftRepository = toppingDraftRepository,
    )

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
}
