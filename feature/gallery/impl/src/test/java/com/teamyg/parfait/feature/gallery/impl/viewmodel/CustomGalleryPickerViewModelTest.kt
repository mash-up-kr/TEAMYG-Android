package com.teamyg.parfait.feature.gallery.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
import com.teamyg.parfait.domain.usecase.member.CompleteTutorialUseCase
import com.teamyg.parfait.domain.usecase.member.GetTutorialVisibleFlowUseCase
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.core.testing.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomGalleryPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getRecentCacheImages: GetRecentCacheImagesUseCase = mockk()
    private val loadGroups: LoadFilterYGGalleryImageGroupsUseCase = mockk(relaxed = true)
    private val getTutorialVisible: GetTutorialVisibleFlowUseCase = mockk()
    private val completeTutorial: CompleteTutorialUseCase = mockk(relaxed = true)

    /** 기본값은 "이미 본 사용자" — 튜토리얼이 다른 테스트의 화면 상태에 끼어들지 않게 한다 */
    private val tutorialVisible = MutableStateFlow(false)

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
        every { getTutorialVisible(TutorialKind.UPLOAD) } returns tutorialVisible

        return CustomGalleryPickerViewModel(
            returnResultOnly = returnResultOnly,
            recentImagePick = recentImagePick,
            getRecentCacheImagesUseCase = getRecentCacheImages,
            loadFilterYGGalleryImageGroupsUseCase = loadGroups,
            getTutorialVisibleFlowUseCase = getTutorialVisible,
            completeTutorialUseCase = completeTutorial,
        )
    }

    /**
     * 튜토리얼 구독은 `launchWhileSubscribed` 라 [CustomGalleryPickerViewModel.state] 를 보는
     * 쪽이 있어야 열린다 — 라우트의 `collectAsStateWithLifecycle()` 을 여기서 흉내 낸다.
     */
    private fun TestScope.shownViewModel() =
        createViewModel(recentImagePick = RecentImagePick.CUTOUT).also { viewModel ->
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()
        }

    @Test
    fun tutorial_onFirstEntry_coversTheScreen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아직 업로드 튜토리얼을 보지 않은 사용자
        tutorialVisible.value = true

        // When 화면이 열린다
        val viewModel = shownViewModel()

        // Then 튜토리얼이 뜬다
        assertTrue(viewModel.state.value.isTutorialVisible)
    }

    @Test
    fun tutorial_alreadySeen_doesNotOpen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 본 사용자(기본 스텁)

        // When 화면이 열린다
        val viewModel = shownViewModel()

        // Then 아무것도 덮지 않는다
        assertFalse(viewModel.state.value.isTutorialVisible)
    }

    @Test
    fun tutorial_onConfirm_closesItAndMarksItSeen() = runTest(mainDispatcherRule.dispatcher) {
        // Given 튜토리얼이 떠 있다
        tutorialVisible.value = true
        val viewModel = shownViewModel()

        // When 칩을 누른다
        viewModel.processIntent(CustomGalleryPickerIntent.OnConfirmTutorial)
        advanceUntilIdle()

        // Then 닫히고, 다음 진입부터 뜨지 않도록 저장한다
        assertFalse(viewModel.state.value.isTutorialVisible)
        coVerify(exactly = 1) { completeTutorial(TutorialKind.UPLOAD) }
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
