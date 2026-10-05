package com.teamyg.parfait.feature.gallery.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager.GalleryAccessLevel
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.topping.EnsureDraftSubjectRecordedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.IOException

class CustomGalleryPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getRecentCacheImages: GetRecentCacheImagesUseCase = mockk()
    private val loadGroups: LoadFilterYGGalleryImageGroupsUseCase = mockk(relaxed = true)
    private val ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase = mockk {
        coEvery { this@mockk(any()) } returns true
    }

    private val source = RecentImage(
        uri = "content://recent/a.jpg",
        filePath = "/data/files/recent_images/a.jpg",
        kind = RecentImageKind.SOURCE,
    )
    private val cutout = RecentImage(
        uri = "content://recent/b.png",
        filePath = "/data/files/recent_images/b.png",
        kind = RecentImageKind.CUTOUT,
    )

    private fun createViewModel(
        recentImagePick: RecentImagePick,
        returnResultOnly: Boolean = false,
    ): CustomGalleryPickerViewModel {
        every { getRecentCacheImages() } returns flowOf(listOf(source, cutout))

        return CustomGalleryPickerViewModel(
            returnResultOnly = returnResultOnly,
            recentImagePick = recentImagePick,
            getRecentCacheImagesUseCase = getRecentCacheImages,
            loadFilterYGGalleryImageGroupsUseCase = loadGroups,
            ensureDraftSubjectRecorded = ensureDraftSubjectRecorded,
        )
    }

    @Test
    fun recentImages_whenPickIsSource_hidesCutout() = runTest(mainDispatcherRule.dispatcher) {
        // Given 원본을 고르는 진입(배경 편집)
        val viewModel = createViewModel(recentImagePick = RecentImagePick.SOURCE, returnResultOnly = true)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 알맹이는 안 보인다 — 배경으로 투명 알맹이가 골라지면 안 된다
        assertEquals(listOf(source), viewModel.state.value.recentImages)
    }

    @Test
    fun recentImages_whenPickIsCutout_hidesSource() = runTest(mainDispatcherRule.dispatcher) {
        // Given 알맹이를 고르는 진입(토핑 만들기)
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 원본은 안 보인다
        assertEquals(listOf(cutout), viewModel.state.value.recentImages)
    }

    @Test
    fun recentImages_whenReturnResultOnlyWithCutoutPick_followsPick() = runTest(mainDispatcherRule.dispatcher) {
        // Given 결과만 돌려주면서 알맹이를 고르는 진입
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT, returnResultOnly = true)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 표시 종류는 returnResultOnly 가 아니라 고른 종류가 정한다
        assertEquals(listOf(cutout), viewModel.state.value.recentImages)
    }

    @Test
    fun onClickCutoutImage_draftAligned_navigatesToToppingPlace() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안을 맞출 수 있는 토핑 만들기 진입
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        // When 알맹이를 누른다
        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))

            // Then 토핑 배치로 가고, 초안에 넘기는 것은 uri 가 아니라 절대경로다
            assertEquals(CustomGalleryPickerEffect.NavigateToToppingPlace, awaitItem())
            coVerify(exactly = 1) { ensureDraftSubjectRecorded(cutout.filePath) }
        }
    }

    @Test
    fun onClickCutoutImage_draftNotAligned_showsDraftUnavailable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안을 맞출 수 없다
        coEvery { ensureDraftSubjectRecorded(any()) } returns false
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        // When 알맹이를 누른다
        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))

            // Then 안내를 띄운다
            assertEquals(CustomGalleryPickerEffect.ShowDraftUnavailable, awaitItem())
        }
    }

    @Test
    fun onClickCutoutImage_ensureThrows_showsDraftUnavailable() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안을 맞추다 I/O 가 실패한다
        coEvery { ensureDraftSubjectRecorded(any()) } throws IOException("disk")
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        // When 알맹이를 누른다
        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))

            // Then 안내를 띄운다
            assertEquals(CustomGalleryPickerEffect.ShowDraftUnavailable, awaitItem())
        }
    }

    @Test
    fun onClickCutoutImage_tappedTwice_ensuresOnce() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안 맞추기가 끝나지 않은 채 멈춰 있다
        val gate = CompletableDeferred<Boolean>()
        coEvery { ensureDraftSubjectRecorded(any()) } coAnswers { gate.await() }
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        viewModel.effect.test {
            // When 두 번 누른 뒤 풀어 준다
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))
            gate.complete(true)

            // Then 한 번만 맞추고 이펙트도 한 번이다
            assertEquals(CustomGalleryPickerEffect.NavigateToToppingPlace, awaitItem())
            coVerify(exactly = 1) { ensureDraftSubjectRecorded(cutout.filePath) }
            expectNoEvents()
        }
    }

    @Test
    fun onClickImage_navigatesToPictureConfirm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        // When 원본 사진을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickImage(uri = source.uri))

            // Then 지금까지의 경로 그대로다
            assertEquals(CustomGalleryPickerEffect.NavigateToConfirm(source.uri), awaitItem())
        }
    }

    @Test
    fun permission_whenDeniedOnEntry_requestsSystemDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(GalleryAccessLevel.DENIED))

            assertEquals(CustomGalleryPickerEffect.RequestPermission, awaitItem())
        }
    }

    @Test
    fun permission_whenStillDeniedAfterRequest_doesNotRequestAgain() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(GalleryAccessLevel.DENIED))
            assertEquals(CustomGalleryPickerEffect.RequestPermission, awaitItem())

            // 다이얼로그가 닫힐 때마다 재개 확인이 들어오므로, 여기서 또 띄우면 끝없이 돈다
            viewModel.processIntent(
                CustomGalleryPickerIntent.OnPermissionResult(GalleryAccessLevel.PERMANENTLY_DENIED),
            )
            viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(GalleryAccessLevel.DENIED))

            expectNoEvents()
        }
    }

    @Test
    fun permission_whenPartialOnEntry_doesNotRequest() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(GalleryAccessLevel.PARTIAL))

            expectNoEvents()
        }
    }
}
