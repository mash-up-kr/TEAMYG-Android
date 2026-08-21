package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.image.ClearSegmentationCacheUseCase
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SegmentImageUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val SOURCE_URI = "content://media/external/images/1"
private const val SUBJECT_PATH = "/cache/segmentation/subject.png"
private const val TRIMMED_SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"

class SegmentationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addRecentImage: AddRecentImageUseCase = mockk(relaxed = true)
    private val clearSegmentationCache: ClearSegmentationCacheUseCase = mockk(relaxed = true)
    private val decodeImage: DecodeImageUseCase = mockk()
    private val segmentImage: SegmentImageUseCase = mockk()
    private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxed = true)

    private val bitmapWrapper: BitmapWrapper = mockk(relaxed = true)

    private val success = SegmentationResult(
        subjectImagePath = SUBJECT_PATH,
        trimmedSubjectImagePath = TRIMMED_SUBJECT_PATH,
        subjectBounds = SegmentationBounds(left = 0, top = 0, right = 10, bottom = 10),
    )

    @Before
    fun stubTheHappyPath() {
        coEvery { decodeImage(SOURCE_URI) } returns Result.success(bitmapWrapper)
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success)
    }

    private fun viewModel() = SegmentationViewModel(
        sourceImageUri = SOURCE_URI,
        addRecentImageUseCase = addRecentImage,
        clearSegmentationCacheUseCase = clearSegmentationCache,
        decodeImageUseCase = decodeImage,
        segmentImageUseCase = segmentImage,
        toppingDraftRepository = toppingDraftRepository,
    )

    @Test
    fun init_segmentationSucceeds_publishesSubjectImagePath() = runTest {
        // Given 정상 응답을 주는 유스케이스들
        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 잘라낸 이미지 경로가 상태에 실린다
        val state = viewModel.state.value
        assertEquals(SUBJECT_PATH, state.subjectImagePath)
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
    fun init_decodeFails_tellsTheUserWithoutSegmenting() = runTest {
        // Given URI 가 만료돼 디코드가 실패를 돌려주는 상황
        coEvery { decodeImage(SOURCE_URI) } returns Result.failure(IllegalStateException("broken uri"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 크래시 대신 토스트로 알리고, 로딩에 갇히지 않으며 세그멘테이션은 시도하지 않는다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 0) { segmentImage(any()) }
    }

    @Test
    fun init_segmentationFails_tellsTheUser() = runTest {
        // Given 세그멘테이션이 실패를 돌려주는 상황
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException("no mask"))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 토스트로 알리고 로딩 오버레이는 걷힌다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun init_noSubjectDetected_tellsTheUser() = runTest {
        // Given 성공했지만 감지된 객체가 없는 응답
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success.copy(subjectBounds = null))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 실패로 알린다 — 하이라이트도 다음 화면으로 갈 방법도 없는 화면을 말없이 남기지 않는다
        viewModel.effect.test { assertEquals(SegmentationEffect.ShowError, awaitItem()) }
    }

    @Test
    fun init_cacheClearThrows_stillSegments() = runTest {
        // Given 캐시 정리가 실패하는 상황
        coEvery { clearSegmentationCache() } throws IllegalStateException("cannot delete")

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 지난 파일을 못 지운 것이 이번 흐름을 막지 않는다
        assertEquals(SUBJECT_PATH, viewModel.state.value.subjectImagePath)
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
        assertEquals(SUBJECT_PATH, viewModel.state.value.subjectImagePath)
        viewModel.effect.test { expectNoEvents() }
    }

    @Test
    fun init_segmentationSucceeds_recordsTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세그멘테이션이 성공한다
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success)

        // When 화면이 돈다
        viewModel()
        advanceUntilIdle()

        // Then 알맹이와 재편집 마스크가 초안에 적힌다. 이 시점엔 두른 테두리가 없다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(
                subjectImagePath = TRIMMED_SUBJECT_PATH,
                cutoutImagePath = SUBJECT_PATH,
                borderColorArgb = null,
                borderWidthDp = null,
            )
        }
    }

    @Test
    fun init_segmentationFails_recordsNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세그멘테이션이 실패한다
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException())

        // When 화면이 돈다
        viewModel()
        advanceUntilIdle()

        // Then 초안에 아무것도 적지 않는다 — 다음 화면으로 갈 수 없는 결과다
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
    }
}
