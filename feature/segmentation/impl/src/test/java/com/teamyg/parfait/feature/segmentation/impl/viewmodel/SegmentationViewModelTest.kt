package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.PersistSubjectUseCase
import com.teamyg.parfait.domain.usecase.image.RecoverCandidatesUseCase
import com.teamyg.parfait.domain.usecase.image.SaveBitmapUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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

private const val SOURCE_URI = "content://media/external/images/1"
private const val SUBJECT_PATH = "/cache/segmentation/subject.png"
private const val TRIMMED_SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"
private const val ORIGIN_PATH = "/cache/segmentation/origin.png"
private const val EDITED_TRIMMED_PATH = "/cache/segmentation/edited_trimmed.png"
private const val EDITED_CUTOUT_PATH = "/cache/segmentation/edited_cutout.png"

private const val ORIGIN_LONG_SIDE = 4032

class SegmentationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addRecentImage: AddRecentImageUseCase = mockk(relaxed = true)
    private val clearSegmentationCache: ClearSegmentationCacheUseCase = mockk(relaxed = true)
    private val decodeImage: DecodeImageUseCase = mockk()
    private val segmentImage: SegmentImageUseCase = mockk()
    private val recordToppingDraft: RecordToppingDraftUseCase = mockk(relaxed = true)
    private val persistSubject: PersistSubjectUseCase = mockk()
    private val saveBitmap: SaveBitmapUseCase = mockk()
    private val recoverCandidates: RecoverCandidatesUseCase = mockk()

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

    private val editResult = ToppingEditResult(
        subjectImagePath = EDITED_TRIMMED_PATH,
        cutoutImagePath = EDITED_CUTOUT_PATH,
        borderLayers = listOf(
            ToppingBorderLayer(colorArgb = 0xFF00FF00.toInt(), widthDp = 4f),
            ToppingBorderLayer(colorArgb = 0xFFFF0000.toInt(), widthDp = 8f),
        ),
        sourceLongSide = ORIGIN_LONG_SIDE,
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
        coEvery { saveBitmap(bitmapWrapper) } returns Result.success(ORIGIN_PATH)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        addRecentImageUseCase = addRecentImage,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
        persistSubjectUseCase = persistSubject,
        saveBitmapUseCase = saveBitmap,
        recordToppingDraft = recordToppingDraft,
        recoverCandidatesUseCase = recoverCandidates,
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
        assertFalse(state.isLoading)
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
    fun init_decodeFails_goesBackWithoutSegmenting() = runTest {
        // Given URI 가 만료돼 디코드가 실패를 돌려주는 상황
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IllegalStateException("broken uri"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패 화면 대신 뒤로 보낸다 — 원본이 없으면 이 화면에서 할 수 있는 일이 없다
        assertFalse(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { segmentImage(any()) }
        viewModel.effect.test { assertEquals(SegmentationEffect.GoBack, awaitItem()) }
    }

    @Test
    fun init_segmentationFails_tellsTheUser() = runTest {
        // Given 세그멘테이션이 실패를 돌려주는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패 화면으로 바뀌고 로딩 오버레이는 걷힌다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun init_noSubjectDetected_tellsTheUser() = runTest {
        // Given 성공했지만 후보가 하나도 없는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(emptyList())

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패 화면으로 바꾼다 — 하이라이트도 다음 화면으로 갈 방법도 없는 화면을 말없이 남기지 않는다
        assertTrue(viewModel.state.value.isError)
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
        coVerify(exactly = 0) { recordToppingDraft(any(), any(), any(), any(), any()) }
    }

    @Test
    fun clickCandidate_succeeds_recordsTheDraftBeforeNavigating() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery {
            recordToppingDraft(any(), any(), any(), any(), any())
        } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 초안을 다 적은 뒤에 이동한다 — 순서가 뒤집히면 확인 화면이 초안 없음으로 잠긴 채 뜬다
        coVerifyOrder {
            persistSubject(candidate)
            recordToppingDraft(
                subjectImagePath = TRIMMED_SUBJECT_PATH,
                cutoutImagePath = SUBJECT_PATH,
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = success.sourceLongSide,
            )
        }
        viewModel.effect.test {
            assertEquals(
                SegmentationEffect.GoToConfirm(
                    subjectImagePath = SUBJECT_PATH,
                    trimmedSubjectImagePath = TRIMMED_SUBJECT_PATH,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun selectCandidate_recordsSourceLongSideFromResult() = runTest {
        // Given 누끼 저장이 원본 긴 변을 함께 돌려준다
        coEvery { persistSubject(any()) } returns Result.success(
            SegmentationResult(
                subjectImagePath = "/cache/canvas.png",
                trimmedSubjectImagePath = "/cache/trimmed.png",
                sourceLongSide = SourceLongSide(4032),
            ),
        )
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 고른다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 그 값이 초안에 실린다
        coVerify {
            recordToppingDraft(
                subjectImagePath = "/cache/trimmed.png",
                cutoutImagePath = "/cache/canvas.png",
                borderColorArgb = null,
                borderWidthDp = null,
                sourceLongSide = SourceLongSide(4032),
            )
        }
    }

    @Test
    fun clickCandidate_succeeds_releasesTheLoadingOverlay() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 로딩이 걷힌다 — 이동이 goTo 라 이 화면이 백스택에 남고, 켠 채 나가면 돌아왔을 때 갇힌다
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun clickCandidate_persisting_showsTheLoadingOverlay() = runTest {
        // Given 저장이 도는 동안 시간이 걸리는 상황
        coEvery { persistSubject(candidate) } coAnswers {
            delay(1_000)
            Result.success(success)
        }
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        runCurrent()

        // Then 저장이 끝나기 전엔 로딩이 켜져 있고, 끝나면 걷힌다
        assertTrue(viewModel.state.value.isLoading)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
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
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun clickCandidate_draftIsNotOpen_doesNotNavigate() = runTest {
        // Given 흐름이 열려 있지 않아 record 가 false 를 돌려주는 상황
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns false
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 후보를 탭한다
        viewModel.processIntent(SegmentationIntent.ClickCandidate(index = 0))
        advanceUntilIdle()

        // Then 이동하지 않고 알린다 — 초안 없이 보내면 확인 화면이 어차피 막힌다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun clickCandidate_tappedTwice_persistsOnlyOnce() = runTest {
        // Given 화면이 열려 후보가 실려 있다
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
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
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
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
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
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
    fun retry_afterFailure_runsTheFlowAgainAndClearsTheError() = runTest {
        // Given 첫 시도가 실패한 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isError)

        // When 다음 시도는 성공하도록 바꾸고 재시도를 누른다
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(listOf(candidate))
        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // Then 실패 표시가 걷히고 후보가 실린다 — 안 걷으면 성공해도 에러 화면이 남는다
        val state = viewModel.state.value
        assertFalse(state.isError)
        assertEquals(listOf(candidate), state.candidates)
        assertFalse(state.isLoading)
    }

    @Test
    fun retry_pressedTwiceWhileRunning_runsOnce() = runTest {
        // Given 세그멘테이션이 오래 걸리는 상황
        coEvery { segmentImage(bitmapWrapper) } coAnswers {
            delay(1_000)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 연달아 두 번 누른다
        viewModel.processIntent(SegmentationIntent.Retry)
        runCurrent()
        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // Then 흐름은 진입 1회 + 재시도 1회로 끝난다 — 두 번째 누름은 버려진다
        coVerify(exactly = 2) { segmentImage(bitmapWrapper) }
    }

    @Test
    fun init_moduleNotReady_marksErrorLikeAnyOtherFailure() = runTest {
        // Given 모듈을 못 받아 실패한 상황
        coEvery { segmentImage(bitmapWrapper) } returns
            Result.failure(SegmentationException.ModuleNotReady(null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 대상 못 찾음과 같은 실패로 접는다 — 디자인이 문구를 한 벌로 요구한다
        assertTrue(viewModel.state.value.isError)
    }

    @Test
    fun editManually_savesOriginOnceAndGoesToEdit() = runTest {
        // Given 세그멘테이션이 실패해 실패 화면이 떠 있다
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 직접 편집을 누른다
        viewModel.processIntent(SegmentationIntent.EditManually)
        advanceUntilIdle()

        // Then 원본만 한 번 저장하고 편집으로 간다. 초안은 아직 적지 않는다
        coVerify(exactly = 1) { saveBitmap(bitmapWrapper) }
        coVerify(exactly = 0) { persistSubject(any()) }
        coVerify(exactly = 0) { recordToppingDraft(any(), any(), any(), any(), any()) }
        assertFalse(viewModel.state.value.isLoading)
        viewModel.effect.test {
            assertEquals(SegmentationEffect.GoToEdit(originImagePath = ORIGIN_PATH), awaitItem())
        }
    }

    @Test
    fun editManually_saveFails_showsToastAndStaysOnErrorScreen() = runTest {
        // Given 실패 화면이 떠 있고 원본 저장이 실패하는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { saveBitmap(bitmapWrapper) } returns Result.failure(IllegalStateException("disk full"))
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 직접 편집을 누른다
        viewModel.processIntent(SegmentationIntent.EditManually)
        advanceUntilIdle()

        // Then 토스트로 알리고 실패 화면에 머문다 — 로딩에 갇히지도 않는다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun editManually_pressedTwiceWhileRunning_runsOnce() = runTest {
        // Given 실패 화면이 떠 있고 원본 저장이 오래 걸리는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { saveBitmap(bitmapWrapper) } coAnswers {
            delay(1_000)
            Result.success(ORIGIN_PATH)
        }
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 연달아 두 번 누른다
        viewModel.processIntent(SegmentationIntent.EditManually)
        runCurrent()
        viewModel.processIntent(SegmentationIntent.EditManually)
        advanceUntilIdle()

        // Then 두 번째 누름은 버려진다 — 같은 원본을 두 벌 떨구지 않는다
        coVerify(exactly = 1) { saveBitmap(bitmapWrapper) }
    }

    @Test
    fun editResult_recordsDraftAndGoesToConfirm() = runTest {
        // Given 직접 편집으로 들어갔던 실패 화면
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집을 마치고 결과가 돌아온다
        viewModel.processIntent(SegmentationIntent.OnEditResult(editResult))
        advanceUntilIdle()

        // Then 가장 바깥 테두리 겹과 원본 긴 변까지 초안에 적고 확인 화면으로 간다
        coVerify(exactly = 1) {
            recordToppingDraft(
                subjectImagePath = EDITED_TRIMMED_PATH,
                cutoutImagePath = EDITED_CUTOUT_PATH,
                borderColorArgb = 0xFFFF0000.toInt(),
                borderWidthDp = 8f,
                sourceLongSide = SourceLongSide(ORIGIN_LONG_SIDE),
            )
        }
        assertFalse(viewModel.state.value.isLoading)
        viewModel.effect.test {
            assertEquals(
                SegmentationEffect.GoToConfirm(
                    subjectImagePath = EDITED_CUTOUT_PATH,
                    trimmedSubjectImagePath = EDITED_TRIMMED_PATH,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun editResult_draftWriteFails_showsToastAndStaysOnErrorScreen() = runTest {
        // Given 초안 기록이 실패하는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns false
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집 결과가 돌아온다
        viewModel.processIntent(SegmentationIntent.OnEditResult(editResult))
        advanceUntilIdle()

        // Then 확인 화면으로 가지 않고 토스트로 알린다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun retry_afterEmptyCandidates_runsTheRecoveryLadder() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(listOf(candidate))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // 1차를 다시 돌지 않는다
        coVerify(exactly = 1) { segmentImage(any()) }
        coVerify(exactly = 1) { recoverCandidates(any()) }
        assertEquals(listOf(candidate), viewModel.state.value.candidates)
        assertFalse(viewModel.state.value.isError)
    }

    @Test
    fun retry_afterAnException_takesTheOriginalPathAgain() = runTest {
        coEvery { segmentImage(any()) } returns Result.failure(SegmentationException.ModuleNotReady(null))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { segmentImage(any()) }
        coVerify(exactly = 0) { recoverCandidates(any()) }
    }

    @Test
    fun retry_afterTheRecoveryAlsoFailed_fallsBackToTheOriginalPath() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(2) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { recoverCandidates(any()) }
        coVerify(exactly = 2) { segmentImage(any()) }
    }

    @Test
    fun retry_pressedFourTimesAfterEmpty_runsTheLadderOnlyOnce() = runTest {
        // Given 같은 사진이라 1차도 회복도 계속 0건이다
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 네 번 누른다 — 1차의 0건이 플래그를 덮으면 세 번째에 사다리가 다시 돈다
        repeat(4) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { recoverCandidates(any()) }
        coVerify(exactly = 4) { segmentImage(any()) }
    }

    @Test
    fun retry_afterTheLadderWasAbortedByTheModule_mayRunTheLadderAgain() = runTest {
        // Given 사다리가 모듈 문제로 중간에 접혔다 — 끝까지 돈 것이 아니다
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } returns Result.failure(SegmentationException.ModuleNotReady(null))
        val viewModel = viewModel()
        advanceUntilIdle()

        repeat(3) {
            viewModel.processIntent(SegmentationIntent.Retry)
            advanceUntilIdle()
        }

        // 회복 → 1차(0건) → 회복
        coVerify(exactly = 2) { recoverCandidates(any()) }
        coVerify(exactly = 2) { segmentImage(any()) }
    }

    @Test
    fun retry_whileTheRecoveryRuns_clearsTheErrorAndShowsLoading() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } coAnswers {
            delay(1_000)
            Result.success(listOf(candidate))
        }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isError)

        viewModel.processIntent(SegmentationIntent.Retry)
        runCurrent()

        // 에러 화면 위에 로딩 덮개가 겹치는 조합을 막는다
        assertFalse(viewModel.state.value.isError)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun retry_recoveryThrowsUnexpectedly_restoresTheErrorScreen() = runTest {
        coEvery { segmentImage(any()) } returns Result.success(emptyList())
        coEvery { recoverCandidates(any()) } throws IllegalStateException("boom")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.processIntent(SegmentationIntent.Retry)
        advanceUntilIdle()

        // 되돌리지 않으면 에러도 후보도 없는 화면에 갇힌다
        assertTrue(viewModel.state.value.isError)
        assertFalse(viewModel.state.value.isLoading)
    }
}
