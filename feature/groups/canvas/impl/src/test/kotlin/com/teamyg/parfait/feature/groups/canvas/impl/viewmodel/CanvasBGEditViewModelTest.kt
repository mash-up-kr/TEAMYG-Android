package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.core.util.android.extension.toRgbHex
import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.usecase.image.UploadImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ChangeCanvasBackgroundUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitDetailUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.groups.canvas.impl.model.EditableTopping
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GROUP_ID = 1L
private const val PARFAIT_ID = 100L

private const val PLACER_GROUP_MEMBER_ID = 11L
private const val OTHER_GROUP_MEMBER_ID = 22L

private const val LOCAL_IMAGE_URI = "content://media/external/images/media/42"
private const val SAVED_IMAGE_URL = "https://cdn.example.com/background.png"

private const val OTHER_PARFAIT_ID = 200L
private const val THIRD_PARFAIT_ID = 300L

class CanvasBGEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTodayParfaitFlow: GetTodayParfaitFlowUseCase = mockk()
    private val refreshTodayParfaitDetail: RefreshTodayParfaitDetailUseCase = mockk()
    private val uploadImage: UploadImageUseCase = mockk()
    private val changeCanvasBackground: ChangeCanvasBackgroundUseCase = mockk()

    /** 저장소의 오늘 캔버스 캐시. 갱신이 성공했다는 것은 여기에 값이 실린다는 뜻이다 */
    private val todayCanvases = MutableStateFlow<CanvasVO?>(null)

    @Before
    fun stubTheHappyPath() {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfaitDetail(any(), any()) } returns Result.success(Unit)
        todayCanvases.value = canvas()
        coEvery { uploadImage(any(), any()) } returns Result.success(ImageId(7L))
    }

    /**
     * 배경 변경은 테스트마다 **보낼 값을 정확히 적어** 스텁한다. MockK 가 sealed interface +
     * value class 조합(`CanvasBackgroundEdit`)에는 `any()` 를 만들지 못하기도 하고, 그 덕에
     * "무엇을 보냈는가"가 스텁 자체로 검증된다 — 다른 값을 보내면 답을 못 찾아 실패한다.
     */
    private fun stubBackgroundChange(
        background: CanvasBackgroundEdit,
        result: Result<CanvasBackground?> = Result.success(CanvasBackground.Color("#FF6B00")),
    ) {
        coEvery { changeCanvasBackground(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), background) } returns result
    }

    /**
     * `launchWhileSubscribed` 는 [CanvasBGEditViewModel.state] 의 구독자 수로 폴링 수명을
     * 잰다 — 라우트의 `collectAsStateWithLifecycle()` 을 흉내 내 여기서 먼저 구독을 붙여야
     * 오늘 캔버스 구독이 열리고 토핑·배경이 시딩된다.
     */
    private fun TestScope.viewModel() = CanvasBGEditViewModel(
        groupIdValue = GROUP_ID,
        parfaitIdValue = PARFAIT_ID,
        getTodayParfaitFlowUseCase = getTodayParfaitFlow,
        refreshTodayParfaitDetailUseCase = refreshTodayParfaitDetail,
        uploadImageUseCase = uploadImage,
        changeCanvasBackgroundUseCase = changeCanvasBackground,
    ).also { viewModel ->
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()
    }

    @Test
    fun init_ordersToppingsByPositionZ() = runTest(mainDispatcherRule.dispatcher) {
        // Given 뒤에 그려야 할 토핑이 목록 앞쪽에 온다
        todayCanvases.value = canvas(
            toppings = listOf(
                topping(parfaitImageId = 1L, positionZ = 5),
                topping(parfaitImageId = 2L, positionZ = 1),
            ),
        )

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 그리는 순서(positionZ 오름차순)로 세운다 — 뒤쪽이 위에 덮인다
        assertEquals(
            listOf(2L, 1L),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
    }

    @Test
    fun init_savedColorBackground_opensOnThatColor() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 배경이 색이다
        todayCanvases.value = canvas(background = CanvasBackground.Color("#FF6B00"))

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 팔레트가 그 색에서 시작한다
        assertEquals(Color(0xFFFF6B00), viewModel.state.value.selectedColor)
        assertNull(viewModel.state.value.selectedImageUri)
    }

    @Test
    fun init_savedImageBackground_opensOnThatImageWithoutASource() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 배경이 이미지다
        todayCanvases.value = canvas(background = CanvasBackground.Image(SAVED_IMAGE_URL))

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 그 이미지를 그리되 출처는 비어 있다 — 기기에 원본이 없다는 표시다
        assertEquals(SAVED_IMAGE_URL, viewModel.state.value.selectedImageUri)
        assertNull(viewModel.state.value.selectedImageSource)
    }

    @Test
    fun observeCanvas_seedsBackgroundSelectionOnlyOnTheFirstEmission() = runTest(mainDispatcherRule.dispatcher) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        todayCanvases.value = canvas(background = CanvasBackground.Image(SAVED_IMAGE_URL))

        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(SAVED_IMAGE_URL, viewModel.state.value.selectedImageUri)

        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(
                uri = LOCAL_IMAGE_URI,
                source = PictureConfirmSource.GALLERY,
            ),
        )
        advanceUntilIdle()

        todayCanvases.value = canvas(background = CanvasBackground.Image("https://cdn.example.com/other.png"))
        advanceUntilIdle()

        assertEquals(LOCAL_IMAGE_URI, viewModel.state.value.selectedImageUri)
        assertEquals(PictureConfirmSource.GALLERY, viewModel.state.value.selectedImageSource)
    }

    @Test
    fun observeCanvas_movesTheEditTargetOnlyOnTheFirstEmission() = runTest(mainDispatcherRule.dispatcher) {
        // 배경을 안 건드려 기본 팔레트 색 그대로인 값을 적는다(stubBackgroundChange 문서 참고)
        val unchangedBackground = CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex())
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery {
            changeCanvasBackground(GroupId(GROUP_ID), ParfaitId(OTHER_PARFAIT_ID), unchangedBackground)
        } returns Result.success(null)
        todayCanvases.value = canvas(parfaitId = OTHER_PARFAIT_ID)

        val viewModel = viewModel()
        advanceUntilIdle()

        // 최초 방출로 편집 대상이 옮겨간 뒤, 다음 방출은 그것을 다시 옮기지 않는다
        todayCanvases.value = canvas(parfaitId = THIRD_PARFAIT_ID)
        advanceUntilIdle()

        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        coVerify { changeCanvasBackground(GroupId(GROUP_ID), ParfaitId(OTHER_PARFAIT_ID), unchangedBackground) }
    }

    @Test
    fun clickConfirm_color_savesBeforeConfirming() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팔레트에서 색을 고른 상태. 서버는 `#RRGGBB` 여섯 자리만 받는다
        stubBackgroundChange(CanvasBackgroundEdit.Color("#FF6B00"))
        val viewModel = viewModel()
        viewModel.processIntent(CanvasBGEditIntent.OnSelectColor(Color(0xFFFF6B00)))

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 저장이 끝난 뒤에야 화면을 넘긴다
            val effect = assertIs<CanvasBGEditEffect.ConfirmBackground>(awaitItem())
            assertEquals(YGCanvasBackground.Solid(Color(0xFFFF6B00)), effect.background)
        }
        coVerify(exactly = 1) {
            changeCanvasBackground(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), CanvasBackgroundEdit.Color("#FF6B00"))
        }
    }

    @Test
    fun clickConfirm_localImage_uploadsThenPatchesWithTheImageId() = runTest(mainDispatcherRule.dispatcher) {
        // Given 기기에서 고른 사진을 배경으로 삼았다
        stubBackgroundChange(
            background = CanvasBackgroundEdit.Image(ImageId(7L)),
            result = Result.success(CanvasBackground.Image(SAVED_IMAGE_URL)),
        )
        val viewModel = viewModel()
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(
                uri = LOCAL_IMAGE_URI,
                source = PictureConfirmSource.GALLERY,
            ),
        )

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 서버가 돌려준 주소로 그린다 — 앱은 imageId 만 알고 URL 은 모른다
            val effect = assertIs<CanvasBGEditEffect.ConfirmBackground>(awaitItem())
            assertEquals(YGCanvasBackground.Image(SAVED_IMAGE_URL), effect.background)
        }
        // 확인까지 마친 imageId 로만 배경을 바꿀 수 있다 — 순서가 뒤집히면 서버가 거절한다
        coVerifyOrder {
            uploadImage(LOCAL_IMAGE_URI, ImageType.BACKGROUND)
            changeCanvasBackground(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), CanvasBackgroundEdit.Image(ImageId(7L)))
        }
    }

    @Test
    fun clickConfirm_untouchedServerImage_doesNotCallTheServer() = runTest(mainDispatcherRule.dispatcher) {
        // Given 저장된 이미지 배경을 그대로 두고 열어 둔 화면
        todayCanvases.value = canvas(background = CanvasBackground.Image(SAVED_IMAGE_URL))
        val viewModel = viewModel()

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 그대로 닫는다
            val effect = assertIs<CanvasBGEditEffect.ConfirmBackground>(awaitItem())
            assertEquals(YGCanvasBackground.Image(SAVED_IMAGE_URL), effect.background)
        }
        // https 주소는 기기에서 읽을 수 없어 올릴 수도 없다
        coVerify(exactly = 0) { uploadImage(any(), any()) }
        verify { changeCanvasBackground wasNot Called }
    }

    @Test
    fun clickConfirm_uploadFails_neitherPatchesNorConfirms() = runTest(mainDispatcherRule.dispatcher) {
        // Given 사진 업로드가 실패한다
        coEvery { uploadImage(any(), any()) } returns Result.failure(AppError.Network(null))
        val viewModel = viewModel()
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(
                uri = LOCAL_IMAGE_URI,
                source = PictureConfirmSource.CAMERA,
            ),
        )

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 화면을 넘기지 않고 실패만 알린다
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.NETWORK), awaitItem())
        }
        verify { changeCanvasBackground wasNot Called }
    }

    @Test
    fun clickConfirm_saveFails_doesNotConfirm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 배경 변경이 서버에서 되돌아온다
        stubBackgroundChange(
            background = CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()),
            result = Result.failure(
                AppError.Server(code = "PARFAIT_NOT_FOUND", statusCode = 404, serverMessage = "없다"),
            ),
        )
        val viewModel = viewModel()

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 저장되지 않은 배경을 저장된 것처럼 넘기지 않는다
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.BACKGROUND_SAVE_UNKNOWN), awaitItem())
        }
    }

    @Test
    fun clickConfirm_unreadableImage_tellsThePhotoIsTheProblem() = runTest(mainDispatcherRule.dispatcher) {
        // Given 고른 사진으로는 올릴 수 없어 업로드 전에 걸렸다
        coEvery { uploadImage(any(), any()) } returns Result.failure(
            AppError.UnsupportedImage(IllegalStateException("서버가 받지 않는 이미지 형식이다")),
        )
        val viewModel = viewModel()
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(
                uri = LOCAL_IMAGE_URI,
                source = PictureConfirmSource.GALLERY,
            ),
        )

        // When 확인
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 다시 눌러도 소용없다는 것을 문구로 가른다
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.UNSUPPORTED_IMAGE), awaitItem())
        }
    }

    @Test
    fun toppings_followEveryEmission() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑이 하나인 캔버스로 열린다
        todayCanvases.value = canvas(toppings = listOf(topping(parfaitImageId = 1L, positionZ = 1)))
        val viewModel = viewModel()
        assertEquals(
            listOf(1L),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )

        // When 두 번째 방출이 토핑을 하나 더 들고 온다
        todayCanvases.value = canvas(
            toppings = listOf(
                topping(parfaitImageId = 1L, positionZ = 1),
                topping(parfaitImageId = 2L, isMine = false, positionZ = 2),
            ),
        )
        advanceUntilIdle()

        // Then 최초 방출 뒤에도 토핑은 그대로 따라온다
        assertEquals(
            listOf(1L, 2L),
            viewModel.state.value.toppings
                .map(EditableTopping::parfaitImageId),
        )
    }

    @Test
    fun clickClose_showsQuitDialog() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.processIntent(CanvasBGEditIntent.OnClickCloseButton)

        assertTrue(viewModel.state.value.showQuitDialog)
    }

    @Test
    fun quitDialogConfirm_emitsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogConfirm)

            assertEquals(CanvasBGEditEffect.NavigateBack, awaitItem())
        }
    }

    private fun canvas(
        parfaitId: Long = PARFAIT_ID,
        background: CanvasBackground? = null,
        toppings: List<CanvasToppingVO> = listOf(
            topping(parfaitImageId = 1L, groupMemberId = PLACER_GROUP_MEMBER_ID, isMine = true, positionZ = 1),
            topping(
                parfaitImageId = OTHER_PARFAIT_IMAGE_ID,
                groupMemberId = OTHER_GROUP_MEMBER_ID,
                isMine = false,
                positionZ = 2,
            ),
        ),
    ) = CanvasVO(
        parfaitId = ParfaitId(parfaitId),
        groupName = GroupName("아메리카노"),
        date = parfaitToday(),
        status = CanvasStatus.ACTIVE,
        lastClosedDate = null,
        members = emptyList(),
        background = background,
        toppings = toppings,
    )

    private fun topping(
        parfaitImageId: Long = 1L,
        groupMemberId: Long = PLACER_GROUP_MEMBER_ID,
        isMine: Boolean = true,
        positionZ: Int = 1,
    ) = CanvasToppingVO(
        parfaitImageId = ParfaitImageId(parfaitImageId),
        imageId = ImageId(parfaitImageId),
        imageUrl = "https://cdn.example.com/topping-$parfaitImageId.png",
        transform = ToppingTransform(
            positionX = 0.25,
            positionY = 0.75,
            positionZ = positionZ,
            scale = 1.0,
            rotation = 0.0,
        ),
        border = ToppingBorder.None,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(groupMemberId),
            nickname = GroupNickname("올린이"),
        ),
        isMine = isMine,
        createdAt = LocalDateTime(2026, 8, 19, 9, 0),
    )

    private companion object {
        const val OTHER_PARFAIT_IMAGE_ID = 2L
    }
}
