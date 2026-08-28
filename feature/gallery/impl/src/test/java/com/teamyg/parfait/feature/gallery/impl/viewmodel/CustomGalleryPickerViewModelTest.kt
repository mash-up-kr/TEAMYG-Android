package com.teamyg.parfait.feature.gallery.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomGalleryPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getRecentCacheImages: GetRecentCacheImagesUseCase = mockk()
    private val loadGroups: LoadFilterYGGalleryImageGroupsUseCase = mockk(relaxed = true)

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

        // Then 원본은 안 보인다 — 한 흐름이 남긴 두 장이 같은 사진으로 나란히 뜨는 것을 막는다
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
    fun onClickCutoutImage_navigatesToSegmentationConfirmWithFilePath() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(recentImagePick = RecentImagePick.CUTOUT)
        advanceUntilIdle()

        // When 알맹이를 누른다
        viewModel.effect.test {
            viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(cutout))

            // Then 확인 화면으로 가고, 넘기는 것은 uri 가 아니라 절대경로다 — 초안 계약이 경로다
            assertEquals(
                CustomGalleryPickerEffect.NavigateToSegmentationConfirm(cutout.filePath),
                awaitItem(),
            )
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
}
