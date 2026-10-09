package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.analytics.AnalyticsLogger
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveBitmapUseCase
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.impl.editor.SubjectMeasure
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.measureSubject
import com.teamyg.parfait.feature.segmentation.impl.editor.trimTo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val SOURCE_URI = "content://media/external/images/1"
private const val SEGMENTATION_URI = "/cache/segmentation/subject.png"
private const val CUTOUT_PATH = "/cache/edited_cutout.png"
private const val TRIMMED_PATH = "/cache/edited_trimmed.png"
private const val CUTOUT_SIDE = 1000

@OptIn(ExperimentalCoroutinesApi::class)
class ToppingEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val decodeImage: DecodeImageUseCase = mockk()
    private val saveBitmap: SaveBitmapUseCase = mockk()
    private val recordToppingDraft: RecordToppingDraftUseCase = mockk()
    private val analyticsLogger: AnalyticsLogger = mockk(relaxed = true)

    private val sourceBitmap: Bitmap = mockk(relaxed = true)
    private val segmentationBitmap: Bitmap = mockk(relaxed = true)
    private val cutout: Bitmap = mockk(relaxed = true) {
        every { width } returns CUTOUT_SIDE
        every { height } returns CUTOUT_SIDE
    }
    private val trimmedCutout: Bitmap = mockk(relaxed = true)

    private val fullMeasure = SubjectMeasure(
        left = 0,
        top = 0,
        right = CUTOUT_SIDE - 1,
        bottom = CUTOUT_SIDE - 1,
        alphaSum = 255L * CUTOUT_SIDE * CUTOUT_SIDE,
    )

    /**
     * 비트맵을 실제로 만드는 편집 헬퍼는 JVM 테스트에서 돌지 않아 통째로 갈아 끼운다.
     * 그래서 여기서 검증되는 것은 저장 이후의 순서이고, 픽셀 합성은 아니다.
     */
    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns Result.success(sourceBitmap.toAndroidBitmap())
        coEvery { decodeImage(SEGMENTATION_URI) } returns Result.success(segmentationBitmap.toAndroidBitmap())

        mockkStatic("com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditMaskKt")
        every { buildCutoutBitmap(any(), any(), any()) } returns cutout
        every { cutout.measureSubject() } returns fullMeasure
        every { cutout.trimTo(fullMeasure) } returns trimmedCutout

        coEvery { saveBitmap(cutout.toAndroidBitmap()) } returns Result.success(CUTOUT_PATH)
        coEvery { saveBitmap(trimmedCutout.toAndroidBitmap()) } returns Result.success(TRIMMED_PATH)
        coEvery { recordToppingDraft(any(), any(), any()) } returns true
    }

    /** `mockkStatic` 은 JVM 전역 상태라 다음 테스트로 새지 않도록 매번 걷어 낸다 */
    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createViewModel(
        sourceImageUri: String = SOURCE_URI,
        segmentationImageUri: String = SEGMENTATION_URI,
        isDetectionFailed: Boolean = false,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = ToppingEditViewModel(
        sourceImageUri = sourceImageUri,
        segmentationImageUri = segmentationImageUri,
        isDetectionFailed = isDetectionFailed,
        savedStateHandle = savedStateHandle,
        decodeImageUseCase = decodeImage,
        saveBitmapUseCase = saveBitmap,
        recordToppingDraft = recordToppingDraft,
        analyticsLogger = analyticsLogger,
    )

    @Test
    fun loadImages_equalUris_sharesOneDecodedBitmap() = runTest {
        // Given 원본과 분석 결과가 같은 주소다
        val viewModel = createViewModel(sourceImageUri = SOURCE_URI, segmentationImageUri = SOURCE_URI)

        // When 이미지를 불러온다
        advanceUntilIdle()

        // Then 한 번만 풀어 같은 비트맵을 함께 쓴다
        val state = viewModel.state.value
        assertSame(sourceBitmap, state.originBitmap)
        assertSame(state.originBitmap, state.segmentationBitmap)
        coVerify(exactly = 1) { decodeImage(any()) }
    }

    @Test
    fun loadImages_differentUris_decodesEach() = runTest {
        // Given 원본과 분석 결과의 주소가 다르다
        val viewModel = createViewModel()

        // When 이미지를 불러온다
        advanceUntilIdle()

        // Then 주소마다 한 번씩 푼다
        val state = viewModel.state.value
        assertSame(sourceBitmap, state.originBitmap)
        assertSame(segmentationBitmap, state.segmentationBitmap)
        coVerify(exactly = 1) { decodeImage(SOURCE_URI) }
        coVerify(exactly = 1) { decodeImage(SEGMENTATION_URI) }
    }

    @Test
    fun loadImages_decodeFails_reportsLoadFailed() = runTest {
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IOException("decode failed"))
        val viewModel = createViewModel()

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())
        }
    }

    @Test
    fun init_detectionFailed_showsGuideOnceAfterLoading() = runTest {
        val viewModel = createViewModel(isDetectionFailed = true)

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.ShowDetectionFailed, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun init_detectionFailedRecreatedWithSameSavedState_doesNotShowAgain() = runTest {
        val savedStateHandle = SavedStateHandle()
        val first = createViewModel(isDetectionFailed = true, savedStateHandle = savedStateHandle)
        first.effect.test {
            assertEquals(ToppingEditEffect.ShowDetectionFailed, awaitItem())
        }

        val second = createViewModel(isDetectionFailed = true, savedStateHandle = savedStateHandle)
        advanceUntilIdle()

        assertFalse(second.state.value.isLoading)
        second.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun init_detectionFailedButLoadFails_onlyReportsLoadFailed() = runTest {
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IOException("decode failed"))
        val viewModel = createViewModel(isDetectionFailed = true)

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun init_notDetectionFailed_showsNothing() = runTest {
        val viewModel = createViewModel(isDetectionFailed = false)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        viewModel.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun clickDone_recordsDraftThenGoesToPlace() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)

            assertEquals(ToppingEditEffect.GoToPlace, awaitItem())
            assertFalse(viewModel.state.value.isSaving)
        }

        coVerify(exactly = 1) {
            recordToppingDraft(
                subjectImagePath = TRIMMED_PATH,
                cutoutImagePath = CUTOUT_PATH,
                sourceLongSide = SourceLongSide(CUTOUT_SIDE),
            )
        }
    }

    @Test
    fun clickDone_recordReturnsFalse_showsDraftUnavailable() = runTest {
        coEvery { recordToppingDraft(any(), any(), any()) } returns false
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)

            assertEquals(ToppingEditEffect.DraftUnavailable, awaitItem())
            assertFalse(viewModel.state.value.isSaving)
        }
    }

    @Test
    fun clickDone_recordThrows_showsSaveFailed() = runTest {
        coEvery { recordToppingDraft(any(), any(), any()) } throws IOException("disk full")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)

            assertEquals(ToppingEditEffect.SaveFailed, awaitItem())
            assertFalse(viewModel.state.value.isSaving)
        }
    }

    @Test
    fun clickDone_saveFails_showsSaveFailedWithoutRecording() = runTest {
        coEvery { saveBitmap(trimmedCutout.toAndroidBitmap()) } returns Result.failure(IOException("disk full"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)

            assertEquals(ToppingEditEffect.SaveFailed, awaitItem())
            assertFalse(viewModel.state.value.isSaving)
        }

        coVerify(exactly = 0) { recordToppingDraft(any(), any(), any()) }
    }

    @Test
    fun clickDone_subjectTooSmall_emitsSubjectTooSmallAndSavesNothing() = runTest {
        // Given 알파가 전부 0 이라 남은 영역이 하한에 못 미친다 (편집 헬퍼를 갈아 끼웠으므로 측정값으로 만든다)
        every { cutout.measureSubject() } returns fullMeasure.copy(alphaSum = 0L)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)

            assertEquals(ToppingEditEffect.SubjectTooSmall, awaitItem())
            assertFalse(viewModel.state.value.isSaving)
        }

        coVerify(exactly = 0) { saveBitmap(any()) }
        coVerify(exactly = 0) { recordToppingDraft(any(), any(), any()) }
    }

    @Test
    fun clickDone_tappedAgainWhileRecording_isIgnored() = runTest {
        // Given 파일 저장은 끝났고 초안 기록이 아직 돌고 있다
        val recordEntered = CompletableDeferred<Unit>()
        val releaseRecord = CompletableDeferred<Unit>()
        coEvery { recordToppingDraft(any(), any(), any()) } coAnswers {
            recordEntered.complete(Unit)
            releaseRecord.await()
            true
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)
            recordEntered.await()
            assertTrue(viewModel.state.value.isSaving)

            // When 그 사이에 완료를 한 번 더 누른다
            viewModel.processIntent(ToppingEditIntent.ClickDone)
            releaseRecord.complete(Unit)

            // Then 저장도 기록도 한 번뿐이다
            assertEquals(ToppingEditEffect.GoToPlace, awaitItem())
            advanceUntilIdle()
            expectNoEvents()
        }

        verify(exactly = 1) { buildCutoutBitmap(any(), any(), any()) }
        coVerify(exactly = 1) { recordToppingDraft(any(), any(), any()) }
    }

    @Test
    fun clickDone_afterReturningFromPlace_completesAgain() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickDone)
            assertEquals(ToppingEditEffect.GoToPlace, awaitItem())

            viewModel.processIntent(ToppingEditIntent.ClickDone)
            assertEquals(ToppingEditEffect.GoToPlace, awaitItem())
        }

        coVerify(exactly = 2) { recordToppingDraft(any(), any(), any()) }
    }

    @Test
    fun clickClose_showsQuitDialog() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.processIntent(ToppingEditIntent.ClickClose)

        assertTrue(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun dismissQuit_hidesQuitDialog() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickClose)
            viewModel.processIntent(ToppingEditIntent.DismissQuit)

            assertFalse(viewModel.state.value.showQuitDialog)
            expectNoEvents()
        }
    }

    @Test
    fun confirmQuit_hidesDialogAndQuitsToCanvas() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickClose)
            viewModel.processIntent(ToppingEditIntent.ConfirmQuit)

            assertFalse(viewModel.state.value.showQuitDialog)
            assertEquals(ToppingEditEffect.QuitToCanvas, awaitItem())
        }
    }

    @Test
    fun confirmQuit_twice_quitsOnce() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(ToppingEditIntent.ClickClose)
            viewModel.processIntent(ToppingEditIntent.ConfirmQuit)
            viewModel.processIntent(ToppingEditIntent.ConfirmQuit)

            assertEquals(ToppingEditEffect.QuitToCanvas, awaitItem())
            expectNoEvents()
        }
    }
}
