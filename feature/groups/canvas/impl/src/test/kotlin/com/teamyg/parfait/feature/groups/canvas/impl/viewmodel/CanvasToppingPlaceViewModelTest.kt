package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule

/** ViewModel 안의 private 상수(TOPPING_DRAG_PX_PER_SCALE=300f, TOPPING_DRAG_DEGREES_PER_PX=0.5f)와 맞춘 값 */
private const val DRAG_PX_PER_SCALE = 300f
private const val DRAG_DEGREES_PER_PX = 0.5f
private const val SCALE_DELTA = 1e-4f

class CanvasToppingPlaceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toppingDraftRepository: ToppingDraftRepository = mockk()

    private fun draft(
        subjectImagePath: String? = "/cache/segmentation/subject.png",
        borderColorArgb: Int? = null,
        borderWidthDp: Float? = null,
    ) = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 3,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = "/cache/segmentation/cutout.png",
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )

    private fun viewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        return CanvasToppingPlaceViewModel(toppingDraftRepository = toppingDraftRepository)
    }

    /** 회전 0/90/180/270도에서 핸들이 가리키는 바깥쪽 방향. [resizeOutwardDirection]과 같은 값이다 */
    private fun outwardDirectionAt(rotationDegrees: Float): Offset {
        val base = 1f / sqrt(2f)
        return when (rotationDegrees) {
            0f -> Offset(base, -base)
            90f -> Offset(base, base)
            180f -> Offset(-base, base)
            270f -> Offset(-base, -base)
            else -> error("이 테스트는 0/90/180/270도만 다룬다: $rotationDegrees")
        }
    }

    private fun rotatedViewModel(rotationDegrees: Float): CanvasToppingPlaceViewModel {
        val viewModel = viewModel()
        if (rotationDegrees != 0f) {
            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = rotationDegrees / DRAG_DEGREES_PER_PX, y = 0f)),
            )
        }
        return viewModel
    }

    @Test
    fun onToppingResizeDrag_atRotation0_matchesExpectedFormula() {
        // Given 회전하지 않은 토핑 (핸들은 우측 상단 모서리에 있다)
        val viewModel = rotatedViewModel(0f)
        val direction = outwardDirectionAt(0f)
        val dragLength = 150f

        // When 핸들을 바깥쪽(우측 상단 대각선)으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(
                Offset(
                    x = dragLength * direction.x,
                    y =
                    dragLength * direction.y,
                ),
            ),
        )

        // Then 드래그 거리를 바깥쪽 방향에 정사영한 만큼 커진다
        val expectedDeltaScale = dragLength / DRAG_PX_PER_SCALE
        assertEquals(1f + expectedDeltaScale, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingResizeDrag_atEachBoundaryRotation_draggingOutwardIncreasesScale() {
        // Given·When 회전 0/90/180/270도 각각에서, 그 회전에서의 바깥쪽 방향으로 핸들을 끈다
        listOf(0f, 90f, 180f, 270f).forEach { rotationDegrees ->
            val viewModel = rotatedViewModel(rotationDegrees)
            val direction = outwardDirectionAt(rotationDegrees)
            val dragLength = 150f

            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingResizeDrag(
                    Offset(x = dragLength * direction.x, y = dragLength * direction.y),
                ),
            )

            // Then 어느 회전에서든 바깥쪽으로 끌면 커진다
            assertTrue(
                viewModel.state.value.scale > 1f,
                "rotation=$rotationDegrees 에서 바깥쪽 드래그가 커지지 않았다: ${viewModel.state.value.scale}",
            )
        }
    }

    @Test
    fun onToppingResizeDrag_atEachBoundaryRotation_draggingInwardDecreasesScale() {
        // Given·When 회전 0/90/180/270도 각각에서, 그 회전에서의 안쪽(바깥쪽의 반대) 방향으로 핸들을 끈다
        listOf(0f, 90f, 180f, 270f).forEach { rotationDegrees ->
            val viewModel = rotatedViewModel(rotationDegrees)
            val direction = outwardDirectionAt(rotationDegrees)
            val dragLength = 150f

            viewModel.processIntent(
                CanvasToppingPlaceIntent.OnToppingResizeDrag(
                    Offset(x = -dragLength * direction.x, y = -dragLength * direction.y),
                ),
            )

            // Then 어느 회전에서든 안쪽으로 끌면 작아진다
            assertTrue(
                viewModel.state.value.scale < 1f,
                "rotation=$rotationDegrees 에서 안쪽 드래그가 작아지지 않았다: ${viewModel.state.value.scale}",
            )
        }
    }

    @Test
    fun onToppingResizeDrag_clampsAtMaxScale() {
        // Given 기본 배율
        val viewModel = viewModel()

        // When 최대 배율을 훌쩍 넘는 크기로 바깥쪽으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(Offset(x = 10_000f, y = -10_000f)),
        )

        // Then 상한(2.5)에서 멈춘다
        assertEquals(2.5f, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingResizeDrag_clampsAtMinScale() {
        // Given 기본 배율
        val viewModel = viewModel()

        // When 최소 배율보다 훨씬 작아지도록 안쪽으로 끈다
        viewModel.processIntent(
            CanvasToppingPlaceIntent.OnToppingResizeDrag(Offset(x = -10_000f, y = 10_000f)),
        )

        // Then 하한(0.5)에서 멈춘다
        assertEquals(0.5f, viewModel.state.value.scale, SCALE_DELTA)
    }

    @Test
    fun onToppingRotateDrag_accumulatesAcrossMultipleDrags() {
        // Given 기본 상태(회전 0도)
        val viewModel = viewModel()

        // When 여러 번에 걸쳐 회전 핸들을 끈다
        viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = 40f, y = 0f)))
        viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingRotateDrag(Offset(x = 20f, y = 0f)))

        // Then 각 드래그의 각도 변화가 그대로 누적된다
        assertEquals((40f + 20f) * DRAG_DEGREES_PER_PX, viewModel.state.value.rotationDegrees, SCALE_DELTA)
    }

    @Test
    fun draft_fillsTheToppingImageAndBorder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 테두리까지 적힌 초안
        val viewModel = viewModel(draft(borderColorArgb = 0xFFFF0000.toInt(), borderWidthDp = 8f))

        // When 화면이 초안을 읽는다
        advanceUntilIdle()

        // Then 올릴 알맹이와 그릴 테두리가 상태에 실린다 — NavKey 인자로 나르지 않는다
        val state = viewModel.state.value
        assertEquals("/cache/segmentation/subject.png", state.toppingImagePath)
        assertEquals(0xFFFF0000.toInt(), state.borderColorArgb)
        assertEquals(8f, state.borderWidthDp)
    }

    @Test
    fun onClickConfirm_withoutASubjectImage_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 가리키던 캐시 파일이 이미 사라졌다
        val viewModel = viewModel(draft(subjectImagePath = null))
        advanceUntilIdle()

        // When 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 조용히 아무 일도 안 하지 않는다 — 올릴 것이 없다고 알린다
            assertEquals(CanvasToppingPlaceEffect.DraftMissing, awaitItem())
        }
    }

    @Test
    fun onClickConfirm_withASubjectImage_confirmsThePlacement() = runTest(mainDispatcherRule.dispatcher) {
        // Given 올릴 알맹이가 있다
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 배치를 확정한다(서버에 올리는 것은 다음 라운드다) — 위치·크기·각도도 그대로 실린다
            val effect = awaitItem()
            assertTrue(effect is CanvasToppingPlaceEffect.ToppingPlaced)
            assertEquals("/cache/segmentation/subject.png", effect.imagePath)
            assertEquals(0.dp, effect.offsetX)
            assertEquals(0.dp, effect.offsetY)
            assertEquals(1f, effect.scale, SCALE_DELTA)
            assertEquals(0f, effect.rotationDegrees, SCALE_DELTA)
        }
    }

    @Test
    fun onClickConfirm_beforeDraftEmits_sendsNoEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안 흐름이 아직 한 번도 방출하지 않았다(DataStore 첫 방출 전 첫 프레임)
        val neverEmittedDraft = MutableSharedFlow<ToppingDraft?>()
        every { toppingDraftRepository.draft } returns neverEmittedDraft
        val viewModel = CanvasToppingPlaceViewModel(toppingDraftRepository = toppingDraftRepository)
        advanceUntilIdle()

        // When 그 상태에서 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 초안이 비었는지 아직 모르므로 거짓 DraftMissing 을 내지 않고 조용히 무시한다
            expectNoEvents()
        }
    }

    @Test
    fun onClickConfirm_afterDraftEmitsWithoutASubjectImage_sendsOnlyDraftMissing() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 초안이 방출됐고 알맹이가 없다
            val viewModel = viewModel(draft(subjectImagePath = null))
            advanceUntilIdle()

            // When 확인을 누른다
            viewModel.effect.test {
                viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
                advanceUntilIdle()

                // Then DraftMissing 하나만 나가고, 뒤이어 되감기 같은 다른 이펙트는 따라오지 않는다
                // (되감을지는 Route 가 정한다 — ViewModel 은 알린다는 사실만 책임진다)
                assertEquals(CanvasToppingPlaceEffect.DraftMissing, awaitItem())
                expectNoEvents()
            }
        }
}
