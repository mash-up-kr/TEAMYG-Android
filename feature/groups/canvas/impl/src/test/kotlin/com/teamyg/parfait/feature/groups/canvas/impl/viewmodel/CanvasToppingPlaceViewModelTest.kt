package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.topping.AddToppingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
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

    private val addToppingUseCase: AddToppingUseCase = mockk()

    private fun viewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        return CanvasToppingPlaceViewModel(
            toppingDraftRepository = toppingDraftRepository,
            addToppingUseCase = addToppingUseCase,
        )
    }

    /** 확정이 나갈 수 있는 최소 조건을 갖춘 ViewModel — 실측 둘 + painter 준비 */
    private fun readyViewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel = viewModel(draft).apply {
        processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(DpSize(360.dp, 640.dp)))
        processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(DpSize(100.dp, 50.dp)))
        processIntent(CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady = true))
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
    fun onClickConfirm_whenImageNotReady_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 실측은 끝났지만 그림은 아직 뜨지 않았다
        val viewModel = viewModel().apply {
            processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(DpSize(360.dp, 640.dp)))
            processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(DpSize(100.dp, 50.dp)))
        }
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 조용히 아무 일도 안 하지 않는다 — 알리고, 폴백 크기로 계산된 배율은 서버에 올리지 않는다
            assertEquals(CanvasToppingPlaceEffect.ToppingImageNotReady, awaitItem())
        }
        coVerify(exactly = 0) { addToppingUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_success_clearsDraftAndNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(CanvasToppingPlaceEffect.PlaceSucceeded, awaitItem())
        }
        // 성공한 흐름의 초안이 남으면 다음 진입까지 낡은 알맹이를 들고 있다
        coVerify(exactly = 1) { toppingDraftRepository.clear() }
    }

    @Test
    fun onClickConfirm_sendsDraftIdentityAndBorderAsServerFormat() = runTest(mainDispatcherRule.dispatcher) {
        val groupIdSlot = slot<GroupId>()
        val parfaitIdSlot = slot<ParfaitId>()
        val transformSlot = slot<ToppingTransform>()
        val borderSlot = slot<ToppingBorder>()
        coEvery {
            addToppingUseCase(
                groupId = capture(groupIdSlot),
                parfaitId = capture(parfaitIdSlot),
                filePath = any(),
                transform = capture(transformSlot),
                border = capture(borderSlot),
            )
        } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit

        val viewModel = readyViewModel(
            draft(borderColorArgb = Color(0xFFFF6B00).toArgb(), borderWidthDp = 4f),
        )
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // 캔버스 식별값은 흐름 진입 때 못 박은 초안 것이다 — 화면이 다시 고르지 않는다
        assertEquals(GroupId(1L), groupIdSlot.captured)
        assertEquals(ParfaitId(2L), parfaitIdSlot.captured)
        assertEquals(3, transformSlot.captured.positionZ)
        // 서버 형식은 Int.toRgbHexString() KDoc 참고
        assertEquals(ToppingBorder.Solid(color = "#FF6B00", width = 4.0), borderSlot.captured)
    }

    @Test
    fun onClickConfirm_withoutBorderColor_sendsNone() = runTest(mainDispatcherRule.dispatcher) {
        val borderSlot = slot<ToppingBorder>()
        coEvery {
            addToppingUseCase(any(), any(), any(), any(), border = capture(borderSlot))
        } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit

        val viewModel = readyViewModel(draft(borderColorArgb = null, borderWidthDp = null))
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // 색이나 두께가 빠진 SOLID 는 서버가 400 INVALID_BORDER 로 거절한다
        assertEquals(ToppingBorder.None, borderSlot.captured)
    }

    @Test
    fun onClickConfirm_nonOpaqueBorderColor_failsInsteadOfCrashing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팔레트가 아닌 곳에서 반투명 색이 흘러든 초안 — 지금은 도달하지 않지만
        // 진입점이 늘면(예: 커스텀 컬러피커) 열리는 경로다
        val viewModel = readyViewModel(draft(borderColorArgb = 0x80FF6B00.toInt(), borderWidthDp = 4f))
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 색 변환이 던져도 크래시가 아니라 실패로 흡수된다
            assertEquals(CanvasToppingPlaceEffect.PlaceFailed, awaitItem())
        }
        // 업로드가 시작되기 전에 끊겨 서버에 고아 이미지가 안 남는다
        coVerify(exactly = 0) { addToppingUseCase(any(), any(), any(), any(), any()) }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onClickConfirm_permanentFailure_rewindsAndKeepsDraftUncleaned() = runTest(mainDispatcherRule.dispatcher) {
        // 스펙의 되감기 표는 세 코드를 든다. 하나만 넣으면 집합이 좁아진 회귀를 못 잡는다
        listOf("PARFAIT_ALREADY_CLOSED", "GROUP_NOT_JOINED", "PARFAIT_NOT_FOUND").forEach { code ->
            coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.failure(
                AppError.Server(code = code, statusCode = null, serverMessage = "서버 메시지"),
            )
            val viewModel = readyViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
                advanceUntilIdle()

                assertEquals(CanvasToppingPlaceEffect.PlaceFailedPermanently, awaitItem(), code)
            }
        }
        // 실패한 흐름의 초안은 남아야 한다 — 비우면 막 만든 토핑을 통째로 잃는다
        coVerify(exactly = 0) { toppingDraftRepository.clear() }
    }

    @Test
    fun onClickConfirm_transientFailure_staysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Network(IOException("connection reset")),
        )
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // 재시도가 의미 있는 갈래라 화면에 남는다
            assertEquals(CanvasToppingPlaceEffect.PlaceFailed, awaitItem())
        }
    }

    @Test
    fun onClickConfirm_whileLoading_doesNotStartASecondUpload() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } coAnswers {
            delay(1_000)
            Result.success(mockk())
        }
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // CONFIRM_JOB_KEY 의 존재 이유(ViewModel KDoc 참고)
        coVerify(exactly = 1) { addToppingUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_setsLoadingWhileInFlight() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } coAnswers {
            delay(1_000)
            Result.success(mockk())
        }
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceTimeBy(500)
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun onClickConfirm_beforeDraftEmits_sendsNoEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안 흐름이 아직 한 번도 방출하지 않았다(DataStore 첫 방출 전 첫 프레임)
        val neverEmittedDraft = MutableSharedFlow<ToppingDraft?>()
        every { toppingDraftRepository.draft } returns neverEmittedDraft
        val viewModel = CanvasToppingPlaceViewModel(
            toppingDraftRepository = toppingDraftRepository,
            addToppingUseCase = addToppingUseCase,
        )
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

    @Test
    fun draft_throws_tellsTheUser_insteadOfDyingSilently() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안 흐름이 던진다(DataStore 읽기 실패 등)
        every { toppingDraftRepository.draft } returns flow { throw IllegalStateException("boom") }
        val viewModel = CanvasToppingPlaceViewModel(
            toppingDraftRepository = toppingDraftRepository,
            addToppingUseCase = addToppingUseCase,
        )

        // When 화면이 열린다
        viewModel.effect.test {
            advanceUntilIdle()

            // Then 수집이 조용히 죽지 않고, 이미 있는 이펙트로 사용자에게 알린다
            assertEquals(CanvasToppingPlaceEffect.DraftMissing, awaitItem())
        }
    }
}
