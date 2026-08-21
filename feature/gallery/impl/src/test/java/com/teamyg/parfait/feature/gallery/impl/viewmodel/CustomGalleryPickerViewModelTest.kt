package com.teamyg.parfait.feature.gallery.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
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

    private fun createViewModel(returnResultOnly: Boolean): CustomGalleryPickerViewModel {
        every { getRecentCacheImages() } returns flowOf(listOf(source, cutout))

        return CustomGalleryPickerViewModel(
            returnResultOnly = returnResultOnly,
            getRecentCacheImagesUseCase = getRecentCacheImages,
            loadFilterYGGalleryImageGroupsUseCase = loadGroups,
        )
    }

    @Test
    fun recentImages_whenReturnResultOnly_hidesCutout() = runTest(mainDispatcherRule.dispatcher) {
        // Given 결과만 돌려주는 진입(배경 선택)
        val viewModel = createViewModel(returnResultOnly = true)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 알맹이는 안 보인다 — 배경으로 투명 알맹이가 골라지면 안 된다
        assertEquals(listOf(source), viewModel.state.value.recentImages)
    }

    @Test
    fun recentImages_whenToppingFlow_showsBoth() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(returnResultOnly = false)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 종류를 가리지 않는다
        assertEquals(listOf(source, cutout), viewModel.state.value.recentImages)
    }

    @Test
    fun onClickImage_withCutout_navigatesToSegmentationConfirmWithFilePath() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(returnResultOnly = false)
        advanceUntilIdle()

        // When 알맹이를 누른다
        viewModel.effect.test {
            viewModel.processIntent(
                CustomGalleryPickerIntent.OnClickImage(uri = cutout.uri, kind = RecentImageKind.CUTOUT),
            )

            // Then 확인 화면으로 가고, 넘기는 것은 uri 가 아니라 절대경로다 — 초안 계약이 경로다
            assertEquals(
                CustomGalleryPickerEffect.NavigateToSegmentationConfirm(cutout.filePath),
                awaitItem(),
            )
        }
    }

    @Test
    fun onClickImage_withSource_navigatesToPictureConfirm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(returnResultOnly = false)
        advanceUntilIdle()

        // When 원본 사진을 누른다
        viewModel.effect.test {
            viewModel.processIntent(
                CustomGalleryPickerIntent.OnClickImage(uri = source.uri, kind = RecentImageKind.SOURCE),
            )

            // Then 지금까지의 경로 그대로다
            assertEquals(CustomGalleryPickerEffect.NavigateToConfirm(source.uri), awaitItem())
        }
    }
}
