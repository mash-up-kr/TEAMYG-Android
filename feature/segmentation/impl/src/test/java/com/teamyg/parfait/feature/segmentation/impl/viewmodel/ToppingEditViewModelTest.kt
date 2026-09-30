package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.image.DecodeImageUseCase
import com.teamyg.parfait.domain.usecase.image.SaveBitmapUseCase
import com.teamyg.parfait.domain.usecase.topping.RecordToppingDraftUseCase
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SOURCE_URI = "content://media/external/images/1"
private const val SEGMENTATION_URI = "/cache/segmentation/subject.png"

class ToppingEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val decodeImage: DecodeImageUseCase = mockk {
        coEvery { this@mockk(any()) } returns Result.failure(IOException("decode failed"))
    }
    private val saveBitmap: SaveBitmapUseCase = mockk()
    private val recordToppingDraft: RecordToppingDraftUseCase = mockk()

    private val result = ToppingEditResult(
        subjectImagePath = "/cache/edited_trimmed.png",
        cutoutImagePath = "/cache/edited_cutout.png",
        borderLayers = emptyList(),
        sourceLongSide = 4032,
    )

    private fun createViewModel(
        sourceImageUri: String = SOURCE_URI,
        segmentationImageUri: String = SEGMENTATION_URI,
    ) = ToppingEditViewModel(
        sourceImageUri = sourceImageUri,
        segmentationImageUri = segmentationImageUri,
        initialBorderLayers = emptyList(),
        borderOnly = false,
        decodeImageUseCase = decodeImage,
        saveBitmapUseCase = saveBitmap,
        recordToppingDraft = recordToppingDraft,
    )

    @Test
    fun loadImages_equalUris_decodesOnce() = runTest {
        // Given 원본과 분석 결과가 같은 주소다(RecordAndConfirm 진입)
        val viewModel = createViewModel(sourceImageUri = SOURCE_URI, segmentationImageUri = SOURCE_URI)

        // When 이미지를 불러온다
        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())
        }

        // Then 같은 주소를 두 번 풀지 않는다
        coVerify(exactly = 1) { decodeImage(SOURCE_URI) }
    }

    @Test
    fun recordResult_recorded_goesToConfirmWithSwappedPaths() = runTest {
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = createViewModel()

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())

            viewModel.processIntent(ToppingEditIntent.RecordResult(result))

            // 편집 결과와 확인 화면 키는 경로 이름이 서로 반대다
            assertEquals(
                ToppingEditEffect.GoToConfirm(
                    subjectImagePath = result.cutoutImagePath,
                    trimmedSubjectImagePath = result.subjectImagePath,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun recordResult_recordReturnsFalse_showsSaveFailed() = runTest {
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns false
        val viewModel = createViewModel()

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())

            viewModel.processIntent(ToppingEditIntent.RecordResult(result))

            assertEquals(ToppingEditEffect.SaveFailed, awaitItem())
        }
    }

    @Test
    fun recordResult_recordThrows_showsSaveFailed() = runTest {
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } throws IOException("disk full")
        val viewModel = createViewModel()

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())

            viewModel.processIntent(ToppingEditIntent.RecordResult(result))

            assertEquals(ToppingEditEffect.SaveFailed, awaitItem())
            assertEquals(false, viewModel.state.value.isSaving)
        }
    }

    @Test
    fun recordResult_afterFirstCompletes_recordsAgain() = runTest {
        coEvery { recordToppingDraft(any(), any(), any(), any(), any()) } returns true
        val viewModel = createViewModel()
        val confirm = ToppingEditEffect.GoToConfirm(
            subjectImagePath = result.cutoutImagePath,
            trimmedSubjectImagePath = result.subjectImagePath,
        )

        viewModel.effect.test {
            assertEquals(ToppingEditEffect.LoadFailed, awaitItem())

            viewModel.processIntent(ToppingEditIntent.RecordResult(result))
            assertEquals(confirm, awaitItem())

            // 같은 key 의 launch 는 앞 job 이 살아 있으면 버려지므로 첫 결과를 받은 뒤에 보낸다
            viewModel.processIntent(ToppingEditIntent.RecordResult(result))
            assertEquals(confirm, awaitItem())
        }

        coVerify(exactly = 2) { recordToppingDraft(any(), any(), any(), any(), any()) }
    }
}
