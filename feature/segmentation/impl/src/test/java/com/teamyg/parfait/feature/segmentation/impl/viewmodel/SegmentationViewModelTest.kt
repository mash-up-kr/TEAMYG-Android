package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

private const val SOURCE_URI = "content://media/external/images/1"
private const val SUBJECT_PATH = "/cache/segmentation/subject.png"
private const val TRIMMED_SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"

private const val ORIGIN_LONG_SIDE = 4032

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addRecentImage: AddRecentImageUseCase = mockk(relaxed = true)
    private val clearSegmentationCache: ClearSegmentationCacheUseCase = mockk(relaxed = true)
    private val decodeImage: DecodeImageUseCase = mockk()
    private val segmentImage: SegmentImageUseCase = mockk()
    private val persistSubject: PersistSubjectUseCase = mockk()

    private val originBitmap: Bitmap = mockk<Bitmap> {
        every { width } returns 3024
        every { height } returns ORIGIN_LONG_SIDE
    }
    private val bitmapWrapper: BitmapWrapper = originBitmap.toAndroidBitmap()

    private val candidate = SegmentationCandidate(
        bounds = SegmentationBounds(left = 0, top = 0, right = 10, bottom = 10),
        bitmap = bitmapWrapper,
        canvasWidth = 100,
        canvasHeight = 100,
        coverageAlphaSum = 255L * 10_000,
    )

    private val secondCandidate = SegmentationCandidate(
        bounds = SegmentationBounds(left = 20, top = 20, right = 30, bottom = 30),
        bitmap = bitmapWrapper,
        canvasWidth = 100,
        canvasHeight = 100,
        coverageAlphaSum = 255L * 10_000,
    )

    private val success = SegmentationResult(
        subjectImagePath = SUBJECT_PATH,
        trimmedSubjectImagePath = TRIMMED_SUBJECT_PATH,
        sourceLongSide = SourceLongSide(2048),
    )

    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns Result.success(bitmapWrapper)
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate))
        coEvery { persistSubject(candidate) } returns Result.success(success)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        addRecentImageUseCase = addRecentImage,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
        persistSubjectUseCase = persistSubject,
    )

    @Test
    fun init_segmentationSucceeds_publishesCandidates() = runTest {
        // Given 정상 응답을 주는 유스케이스들
        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 후보 목록이 상태에 실린다
        val state = viewModel.state.value
        assertEquals(listOf(candidate), state.candidates)
        assertFalse(state.isAnalyzing)
        assertFalse(state.isSaving)
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun init_always_clearsTheCacheBeforeDecoding() = runTest {
        // Given 정상 응답
        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 정리가 디코드보다 먼저다 — 뒤에 두면 이번 흐름이 방금 만든 파일을 지운다
        coVerifyOrder {
            clearSegmentationCache()
            decodeImage(SOURCE_URI)
        }
    }

    @Test
    fun init_cacheClearThrows_stillSegments() = runTest {
        // Given 캐시 정리가 실패하는 상황
        coEvery { clearSegmentationCache() } throws IllegalStateException("cannot delete")

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 지난 파일을 못 지운 것이 이번 흐름을 막지 않는다
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun init_always_recordsTheSourceAsRecentImage() = runTest {
        // Given 정상 응답
        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 최근 목록에 이 흐름이 쓴 원본을 남긴다
        coVerify(exactly = 1) { addRecentImage(SOURCE_URI, RecentImageKind.SOURCE) }
    }

    @Test
    fun init_decodeFails_doesNotRecordTheSourceAsRecentImage() = runTest {
        // Given 열리지 않는 이미지
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IllegalStateException("broken uri"))

        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 최근 목록의 자리를 열리지 않는 이미지에 내주지 않는다
        coVerify(exactly = 0) { addRecentImage(any(), any()) }
    }

    @Test
    fun init_cacheClearIsCancelled_stopsBeforeDecoding() = runTest {
        // Given 화면을 벗어나 캐시 정리가 취소된 상황
        coEvery { clearSegmentationCache() } throws CancellationException("scope gone")

        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 취소는 실패가 아니라 전파돼야 한다 — 값으로 접으면 떠난 화면이 계속 일한다
        coVerify(exactly = 0) { decodeImage(any()) }
    }

    @Test
    fun init_recentImageRecordIsCancelled_stopsBeforeSegmenting() = runTest {
        // Given 화면을 벗어나 최근 이미지 기록이 취소된 상황
        coEvery { addRecentImage(SOURCE_URI, RecentImageKind.SOURCE) } throws CancellationException("scope gone")

        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 곁다리 작업을 감싼 가드도 취소만은 통과시켜야 한다
        coVerify(exactly = 0) { segmentImage(any()) }
    }

    @Test
    fun init_recentImageRecordThrows_stillSegments() = runTest {
        // Given 최근 이미지 기록이 실패하는 상황
        coEvery { addRecentImage(SOURCE_URI, RecentImageKind.SOURCE) } throws IllegalStateException("cannot copy")

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 곁다리 기록의 실패가 잘라내기 자체를 막지 않는다
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun init_multipleCandidatesDetected_publishesAllCandidates() = runTest {
        // Given 후보가 둘 잡히는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate, secondCandidate))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 걸러지지 않은 후보가 모두 화면까지 온다 — 하나로 접히면 고를 수가 없다
        assertEquals(listOf(candidate, secondCandidate), viewModel.state.value.candidates)
    }

    @Test
    fun init_segmentationSucceeds_persistsNothingYet() = runTest {
        // Given 후보가 잡히는 정상 응답
        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 아직 아무것도 떨구지 않는다 — 고르지도 않은 후보를 디스크에 쓰지 않는다
        coVerify(exactly = 0) { persistSubject(any()) }
    }

    @Test
    fun clickCandidate_succeeds_goesToEditWithFullSizeSubject() = runTest {
        // Given 저장 결과가 원본 크기본과 여백을 걷어 낸 저장본을 따로 돌려준다
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 편집에는 원본 크기본을 실어 보낸다 — 초안은 편집이 "다음"에서 적는다
        viewModel.effect.test {
            assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = SUBJECT_PATH), awaitItem())
        }
    }

    @Test
    fun clickCandidate_succeeds_releasesTheLoadingOverlay() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 로딩이 걷힌다 — 이동이 goTo 라 이 화면이 백스택에 남고, 켠 채 나가면 돌아왔을 때 갇힌다
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun clickCandidate_persisting_showsTheLoadingOverlay() = runTest {
        // Given 저장이 도는 동안 시간이 걸리는 상황
        coEvery { persistSubject(candidate) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(success)
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        runCurrent()

        // Then 저장이 끝나기 전엔 로딩이 켜져 있고, 끝나면 걷힌다
        assertTrue(viewModel.state.value.isSaving)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun clickCandidate_persistFails_keepsTheCandidatesAndTellsTheUser() = runTest {
        // Given 저장이 실패하는 상황
        coEvery { persistSubject(candidate) } returns Result.failure(IllegalStateException("no space"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 알리되 목록은 남긴다 — 사용자가 다른 후보를 고를 수 있어야 한다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        assertFalse(viewModel.state.value.isSaving)
    }

    @Test
    fun clickCandidate_tappedTwice_persistsOnlyOnce() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 저장이 끝나기 전에 두 번 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 한 번만 떨군다 — 로딩 오버레이가 터치를 막아 주는지에 기대지 않는다
        coVerify(exactly = 1) { persistSubject(any()) }
    }

    @Test
    fun clickCandidate_indexIsOutOfRange_doesNothing() = runTest {
        // Given 후보가 하나뿐인 상태
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 없는 자리를 가리키는 의도가 들어온다(상태 교체와 탭이 경합하면 생긴다)
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 3))
        advanceUntilIdle()

        // Then 아무 일도 일어나지 않는다
        coVerify(exactly = 0) { persistSubject(any()) }
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun clickCandidate_tapsTheSecondOfTwo_persistsTheTappedCandidate() = runTest {
        // Given 후보가 둘 잡혀 있다
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate, secondCandidate))
        coEvery { persistSubject(secondCandidate) } returns Result.success(success)
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 두 번째 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 1))
        advanceUntilIdle()

        // Then 탭한 후보로 저장되고 첫 번째는 저장되지 않는다
        coVerify(exactly = 1) { persistSubject(secondCandidate) }
        coVerify(exactly = 0) { persistSubject(candidate) }
    }

    @Test
    fun clickCandidate_tappedAgainAfterCompletion_persistsAgain() = runTest {
        // Given 첫 저장이 이미 끝난 상태
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // When 다시 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 중복 탭 가드는 작업이 도는 동안만 막고, 끝난 뒤에는 다시 저장한다
        coVerify(exactly = 2) { persistSubject(candidate) }
    }

    @Test
    fun init_decodeFails_goesToEdit() = runTest {
        // Given URI 가 만료돼 디코드가 실패를 돌려주는 상황
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IllegalStateException("broken uri"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 분석 없이 편집으로 보낸다. 교체 직전 프레임에 빈 선택 UI 가 비치지 않게 분석 상태는 그대로다
        assertTrue(viewModel.state.value.isAnalyzing)
        coVerify(exactly = 0) { segmentImage(any()) }
        viewModel.effect.test { assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = null), awaitItem()) }
    }

    @Test
    fun init_segmentationReturnsFailure_goesToEdit() = runTest {
        // Given 세그멘테이션이 실패를 돌려주는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 편집으로 보낸다
        assertTrue(viewModel.state.value.isAnalyzing)
        viewModel.effect.test { assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = null), awaitItem()) }
    }

    @Test
    fun init_segmentationThrows_goesToEdit() = runTest {
        // Given 세그멘테이션이 Result 로 감싸지 않고 예외를 던지는 상황
        coEvery { segmentImage(any()) } throws IllegalStateException()

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 로딩에 갇히지 않고 편집으로 보낸다
        assertTrue(viewModel.state.value.isAnalyzing)
        viewModel.effect.test { assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = null), awaitItem()) }
    }

    @Test
    fun init_noSubjectDetected_goesToEdit() = runTest {
        // Given 성공했지만 후보가 하나도 없는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(emptyList())

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 편집으로 보낸다
        assertTrue(viewModel.state.value.isAnalyzing)
        viewModel.effect.test { assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = null), awaitItem()) }
    }

    @Test
    fun clickClose_showsQuitDialog() = runTest {
        // Given 분석이 끝난 화면
        val viewModel = viewModel()
        advanceUntilIdle()

        // When X 를 누른다
        viewModel.processIntent(SegmentationIntent.ClickClose)

        // Then 그만두기 팝업이 뜬다
        assertTrue(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun dismissQuit_hidesQuitDialog() = runTest {
        // Given 팝업이 떠 있다
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(SegmentationIntent.ClickClose)

        // When 계속 편집을 누른다
        viewModel.processIntent(SegmentationIntent.DismissQuit)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun confirmQuit_quitsToCanvas() = runTest {
        // Given 팝업이 떠 있다
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(SegmentationIntent.ClickClose)

        // When 그만두기를 누른다
        viewModel.processIntent(SegmentationIntent.ConfirmQuit)
        advanceUntilIdle()

        // Then 캔버스로 나간다
        viewModel.effect.test { assertEquals(SegmentationEffect.QuitToCanvas, awaitItem()) }
        // 전환 동안 팝업이 남지 않는다
        assertEquals(false, viewModel.state.value.showQuitDialog)
    }

    @Test
    fun confirmQuit_twice_quitsOnce() = runTest {
        // Given 팝업이 떠 있다
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.processIntent(SegmentationIntent.ClickClose)

        // When 그만두기를 두 번 누른다
        viewModel.processIntent(SegmentationIntent.ConfirmQuit)
        viewModel.processIntent(SegmentationIntent.ConfirmQuit)
        advanceUntilIdle()

        // Then 캔버스로 나가는 effect 는 한 번뿐이다
        viewModel.effect.test {
            assertEquals(SegmentationEffect.QuitToCanvas, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun candidatesArriveWhileQuitDialogOpen_areHeldUntilDismissed() = runTest {
        // Given 분석이 늦게 끝나는 상황에서 팝업을 연다
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        runCurrent()
        viewModel.processIntent(SegmentationIntent.ClickClose)

        // When 팝업이 떠 있는 채로 분석이 끝난다
        advanceUntilIdle()

        // Then 결과는 보류되고 로딩 화면이 유지된다
        assertTrue(viewModel.state.value.isAnalyzing)
        assertEquals(emptyList(), viewModel.state.value.candidates)

        // When 팝업을 닫는다
        viewModel.processIntent(SegmentationIntent.DismissQuit)

        // Then 보류된 후보가 적용된다
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        assertFalse(viewModel.state.value.isAnalyzing)
    }

    @Test
    fun noSubjectWhileQuitDialogOpen_isHeldUntilDismissed() = runTest {
        // Given 0개로 끝날 분석이 늦게 끝나는 상황에서 팝업을 연다
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(emptyList())
        }
        val viewModel = viewModel()
        runCurrent()
        viewModel.processIntent(SegmentationIntent.ClickClose)
        advanceUntilIdle()

        viewModel.effect.test {
            // Then 보류 중에는 편집으로 가지 않는다
            expectNoEvents()

            // When 팝업을 닫는다
            viewModel.processIntent(SegmentationIntent.DismissQuit)

            // Then 그제야 편집으로 간다
            assertEquals(SegmentationEffect.GoToEdit(segmentationImagePath = null), awaitItem())
        }
    }

    @Test
    fun confirmQuit_withHeldResult_dropsIt() = runTest {
        // Given 팝업이 떠 있는 동안 후보가 보류된 상황
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        runCurrent()
        viewModel.processIntent(SegmentationIntent.ClickClose)
        advanceUntilIdle()

        viewModel.effect.test {
            // When 그만두기를 누르고 팝업이 닫힌다
            viewModel.processIntent(SegmentationIntent.ConfirmQuit)
            viewModel.processIntent(SegmentationIntent.DismissQuit)

            // Then 보류된 결과는 버려진다 — effect 는 QuitToCanvas 하나뿐이고 후보는 적용되지 않는다
            assertEquals(SegmentationEffect.QuitToCanvas, awaitItem())
            expectNoEvents()
        }
        assertEquals(emptyList(), viewModel.state.value.candidates)
    }

    @Test
    fun confirmQuit_beforeAnalysisEnds_ignoresLateResult() = runTest {
        // Given 분석이 도는 중에 그만두기를 확정한다
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        runCurrent()
        viewModel.processIntent(SegmentationIntent.ClickClose)
        viewModel.processIntent(SegmentationIntent.ConfirmQuit)
        viewModel.processIntent(SegmentationIntent.DismissQuit)

        viewModel.effect.test {
            assertEquals(SegmentationEffect.QuitToCanvas, awaitItem())

            // When 분석이 뒤늦게 끝난다
            advanceUntilIdle()

            // Then 추가 effect 도 후보 적용도 없다
            expectNoEvents()
        }
        assertEquals(emptyList(), viewModel.state.value.candidates)
    }

    @Test
    fun confirmQuit_withoutDialog_ignoresLateResult() = runTest {
        // Given 팝업 없이 그만두기가 확정된 상황(팝업이 이미 닫힌 뒤 도착한 의도)
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000.milliseconds)
            Result.success(emptyList())
        }
        val viewModel = viewModel()
        runCurrent()
        viewModel.processIntent(SegmentationIntent.ConfirmQuit)

        viewModel.effect.test {
            assertEquals(SegmentationEffect.QuitToCanvas, awaitItem())

            // When 분석이 뒤늦게 끝난다
            advanceUntilIdle()

            // Then 편집으로 가지 않는다 — QuitToCanvas 뿐이다
            expectNoEvents()
        }
    }
}
