package com.teamyg.parfait.feature.groups.canvas.impl.viewmodel

import android.net.Uri
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
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.usecase.image.UploadImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ChangeCanvasBackgroundUseCase
import com.teamyg.parfait.domain.usecase.parfait.GetTodayParfaitFlowUseCase
import com.teamyg.parfait.domain.usecase.parfait.RefreshTodayParfaitUseCase
import com.teamyg.parfait.domain.usecase.topping.DeleteToppingUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingBorderUseCase
import com.teamyg.parfait.domain.usecase.topping.UpdateToppingUseCase
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.After
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
    private val refreshTodayParfait: RefreshTodayParfaitUseCase = mockk()
    private val uploadImage: UploadImageUseCase = mockk()
    private val changeCanvasBackground: ChangeCanvasBackgroundUseCase = mockk()
    private val deleteTopping: DeleteToppingUseCase = mockk()
    private val updateTopping: UpdateToppingUseCase = mockk()
    private val updateToppingBorder: UpdateToppingBorderUseCase = mockk()

    /** 저장소의 오늘 캔버스 캐시. 갱신이 성공했다는 것은 여기에 값이 실린다는 뜻이다 */
    private val todayCanvases = MutableStateFlow<CanvasVO?>(null)

    @Before
    fun stubTheHappyPath() {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
        todayCanvases.value = canvas()
        coEvery { uploadImage(any(), any()) } returns Result.success(ImageId(7L))
    }

    /** [mockkStatic] 은 JVM 전역 상태라 다음 테스트로 새지 않도록 매번 걷어 낸다 */
    @After
    fun tearDown() {
        unmockkAll()
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

    private fun TestScope.viewModel(initialToppingIdValue: Long? = null) = CanvasBGEditViewModel(
        groupIdValue = GROUP_ID,
        parfaitIdValue = PARFAIT_ID,
        initialToppingIdValue = initialToppingIdValue,
        getTodayParfaitFlowUseCase = getTodayParfaitFlow,
        refreshTodayParfaitUseCase = refreshTodayParfait,
        uploadImageUseCase = uploadImage,
        changeCanvasBackgroundUseCase = changeCanvasBackground,
        deleteToppingUseCase = deleteTopping,
        updateToppingUseCase = updateTopping,
        updateToppingBorderUseCase = updateToppingBorder,
    ).also { advanceUntilIdle() }

    /** 실제 스텁 중 내 것 하나를 선택해 둔다 — 삭제·편집은 내 토핑에서만 열린다 */
    private fun CanvasBGEditViewModel.selectMyTopping(): CanvasToppingItem {
        val topping = state.value.toppings.first { it.isMine }
        processIntent(CanvasBGEditIntent.OnClickTopping(topping))
        return topping
    }

    @Test
    fun init_placesToppingsByTheStoredRatiosAndMarksMine() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 화면이 열린다
        val viewModel = viewModel()

        // Then 저장된 배치를 그대로 들고, 내 토핑만 편집 대상이 된다
        val toppings = viewModel.state.value.toppings
        assertEquals(listOf(1L, 2L), toppings.map(CanvasToppingItem::parfaitImageId))
        assertEquals(listOf(true, false), toppings.map(CanvasToppingItem::isMine))
        assertEquals(0.25f, toppings.first().positionX)
        assertEquals(0.75f, toppings.first().positionY)
    }

    @Test
    fun init_withInitialToppingId_opensToppingTabWithThatToppingSelected() = runTest(mainDispatcherRule.dispatcher) {
        // Given, When 캔버스 메인에서 내 토핑을 탭해 들어온다
        val viewModel = viewModel(initialToppingIdValue = 1L)

        // Then 배경 탭이 아니라 토핑 탭에서, 탭했던 그 토핑이 바로 선택돼 있다
        assertEquals(CanvasEditTab.TOPPING, viewModel.state.value.selectedTab)
        assertEquals(1L, viewModel.state.value.selectedToppingId)
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
                .map(CanvasToppingItem::parfaitImageId),
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
    fun init_canvasFails_showsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스 갱신이 끊긴다
        coEvery { refreshTodayParfait(any(), any()) } returns Result.failure(AppError.Network(null))

        // When 화면이 열린다
        val viewModel = CanvasBGEditViewModel(
            groupIdValue = GROUP_ID,
            parfaitIdValue = PARFAIT_ID,
            initialToppingIdValue = null,
            getTodayParfaitFlowUseCase = getTodayParfaitFlow,
            refreshTodayParfaitUseCase = refreshTodayParfait,
            uploadImageUseCase = uploadImage,
            changeCanvasBackgroundUseCase = changeCanvasBackground,
            deleteToppingUseCase = deleteTopping,
            updateToppingUseCase = updateTopping,
            updateToppingBorderUseCase = updateToppingBorder,
        )

        // Then 조용히 빈 화면을 두지 않고 실패를 알린다
        viewModel.effect.test {
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.NETWORK), awaitItem())
        }
    }

    @Test
    fun observeCanvas_seedsBackgroundSelectionOnlyOnTheFirstEmission() = runTest(mainDispatcherRule.dispatcher) {
        every { getTodayParfaitFlow(any(), any()) } returns todayCanvases
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
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
        coEvery { refreshTodayParfait(any(), any()) } returns Result.success(Unit)
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
            assertEquals(CanvasBGEditEffect.ShowError(CanvasBGEditError.UNKNOWN), awaitItem())
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
    fun onClickConfirm_toppingMoved_updatesOnlyThatTopping() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮긴다. 배경은 안 건드려 기본 팔레트 색 그대로다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        val topping = viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = -0.05f))
        val moved = viewModel.state.value.toppings
            .first { it.parfaitImageId == topping.parfaitImageId }

        coEvery {
            updateTopping(
                groupId = GroupId(GROUP_ID),
                parfaitId = ParfaitId(PARFAIT_ID),
                parfaitImageId = ParfaitImageId(topping.parfaitImageId),
                positionX = moved.positionX.toDouble(),
                positionY = moved.positionY.toDouble(),
                scale = moved.scale.toDouble(),
                rotation = moved.rotationDegrees.toDouble(),
            )
        } returns Result.success(mockk<UpdatedToppingVO>())

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 옮긴 토핑만 저장 요청이 나가고, 안 건드린 토핑은 나가지 않는다
        coVerify(exactly = 1) {
            updateTopping(
                groupId = GroupId(GROUP_ID),
                parfaitId = ParfaitId(PARFAIT_ID),
                parfaitImageId = ParfaitImageId(topping.parfaitImageId),
                positionX = moved.positionX.toDouble(),
                positionY = moved.positionY.toDouble(),
                scale = moved.scale.toDouble(),
                rotation = moved.rotationDegrees.toDouble(),
            )
        }
        coVerify(exactly = 0) {
            updateTopping(any(), any(), ParfaitImageId(OTHER_PARFAIT_IMAGE_ID), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun onClickConfirm_toppingBorderEdited_savesOnlyTheBorder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 건드리지 않고, 테두리 편집 결과만 받는다
        // handleOnToppingEditResult 가 File(...).toUri() 로 cutoutImagePath 를 적는데,
        // 그 안의 Uri.fromFile 은 안드로이드 프레임워크 실물이라 JVM 유닛 테스트에서 스텁 없인 던진다
        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockk(relaxed = true)
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        val topping = viewModel.state.value.toppings
            .first { it.isMine }
        viewModel.processIntent(
            CanvasBGEditIntent.OnToppingEditResult(
                toppingId = topping.parfaitImageId,
                result = ToppingEditResult(
                    subjectImagePath = "/cache/segmentation/subject.png",
                    cutoutImagePath = "/cache/segmentation/cutout.png",
                    borderLayers = listOf(ToppingBorderLayer(colorArgb = 0xFFFF6B00.toInt(), widthDp = 4f)),
                ),
            ),
        )
        val newBorder = ToppingBorder.Solid(color = "#FF6B00", width = 4.0)
        val parfaitImageId = ParfaitImageId(topping.parfaitImageId)
        coEvery {
            updateToppingBorder(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), parfaitImageId, newBorder)
        } returns Result.success(UpdatedToppingBorderVO(parfaitImageId, newBorder))

        // When 확인 버튼을 누른다 — 위치는 안 바뀌었으니 updateTopping 은 부를 필요가 없다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 테두리만 저장 요청이 나간다
        coVerify(exactly = 1) {
            updateToppingBorder(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), parfaitImageId, newBorder)
        }
        coVerify(exactly = 0) { updateTopping(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_noToppingChanges_doesNotCallUpdate() = runTest(mainDispatcherRule.dispatcher) {
        // Given 아무 토핑도 건드리지 않는다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()

        // When 확인 버튼을 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 저장할 게 없으니 API 를 부르지 않는다
        coVerify(exactly = 0) { updateTopping(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_toppingUpdateFails_stillConfirmsBackground() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 옮겼는데 저장은 실패한다
        stubBackgroundChange(CanvasBackgroundEdit.Color(CanvasBackgroundPaletteColors.first().toRgbHex()))
        val viewModel = viewModel()
        viewModel.selectMyTopping()
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = 0f))
        coEvery {
            updateTopping(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("실패"))

        // When 확인 버튼을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm)

            // Then 크래시 없이 배경 확인은 그대로 진행된다
            assertIs<CanvasBGEditEffect.ConfirmBackground>(awaitItem())
        }
    }

    @Test
    fun toppingMoveDrag_movesByTheRatioItReceives() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 고른 상태
        val viewModel = viewModel()
        val mine = viewModel.state.value.toppings
            .first { it.isMine }
        viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(mine))

        // When 캔버스 너비의 10%, 높이의 5% 만큼 끈다
        viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = 0.1f, deltaY = 0.05f))

        // Then 화면 크기와 무관한 비율 그대로 옮겨진다
        val moved = viewModel.state.value.toppings
            .first { it.parfaitImageId == mine.parfaitImageId }
        assertEquals(0.35f, moved.positionX)
        assertEquals(0.80f, moved.positionY)
    }

    @Test
    fun clickTopping_notMine_staysUnselected() = runTest(mainDispatcherRule.dispatcher) {
        // Given 남이 올린 토핑
        val viewModel = viewModel()
        val others = viewModel.state.value.toppings
            .first { it.isMine.not() }

        // When 탭
        viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(others))

        // Then 남의 토핑은 고를 수 없다
        assertNull(viewModel.state.value.selectedToppingId)
    }

    @Test
    fun toppingResize_largeDrag_isNoLongerClampedToTheOldMax() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 고른 상태
        val viewModel = viewModel()
        val mine = viewModel.selectMyTopping()

        // When 예전 상한을 훌쩍 넘도록 크게 키운다
        viewModel.processIntent(CanvasBGEditIntent.OnToppingResize(scaleFactor = 10f))

        // Then 예전 상한(2.5)에 걸리지 않고 그대로 커진다
        val resized = viewModel.state.value.toppings
            .first { it.parfaitImageId == mine.parfaitImageId }
        assertTrue(resized.scale > 2.5f)
    }

    @Test
    fun deleteToppingDialogConfirm_useCaseSucceeds_removesTopping() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 선택했고, 삭제 API 는 성공한다
        val viewModel = viewModel()
        val topping = viewModel.selectMyTopping()
        coEvery {
            deleteTopping(GroupId(GROUP_ID), ParfaitId(PARFAIT_ID), ParfaitImageId(topping.parfaitImageId))
        } returns Result.success(Unit)

        // When 삭제 모달의 "삭제하기" 를 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then 목록에서 사라지고 선택이 풀린다
        assertTrue(
            viewModel.state.value.toppings
                .none { it.parfaitImageId == topping.parfaitImageId },
        )
        assertNull(viewModel.state.value.selectedToppingId)
    }

    @Test
    fun deleteToppingDialogConfirm_useCaseFails_keepsTopping() = runTest(mainDispatcherRule.dispatcher) {
        // Given 내 토핑을 선택했고, 삭제 API 는 실패한다
        val viewModel = viewModel()
        val topping = viewModel.selectMyTopping()
        coEvery { deleteTopping(any(), any(), any()) } returns Result.failure(RuntimeException("실패"))

        // When 삭제 모달의 "삭제하기" 를 누른다
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then 목록은 그대로다 — 서버에 반영되지 않았으므로 화면에서도 지우지 않는다. 크래시도 안 난다
        assertTrue(
            viewModel.state.value.toppings
                .any { it.parfaitImageId == topping.parfaitImageId },
        )
    }

    @Test
    fun deleteToppingDialogConfirm_noSelection_doesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 선택된 토핑이 없다
        val viewModel = viewModel()

        // When 그래도 삭제 확인 인텐트가 온다(방어적 상황)
        viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
        advanceUntilIdle()

        // Then API 를 부르지 않는다
        coVerify(exactly = 0) { deleteTopping(any(), any(), any()) }
    }

    @Test
    fun init_solidBorder_becomesOneEditableLayer() = runTest(mainDispatcherRule.dispatcher) {
        // Given 테두리를 두른 토핑
        todayCanvases.value = canvas(
            toppings = listOf(
                topping(border = ToppingBorder.Solid(color = "#FF6B00", width = 4.0)),
            ),
        )

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 편집 화면이 되살릴 수 있는 겹 하나로 편다
        val layers = viewModel.state.value.toppings
            .first()
            .borderLayers
        assertEquals(1, layers.size)
        assertEquals(4f, layers.first().widthDp)
    }

    @Test
    fun init_unreadableBorderColor_drawsNoBorder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 색 문자열을 읽을 수 없는 테두리
        todayCanvases.value = canvas(
            toppings = listOf(
                topping(border = ToppingBorder.Solid(color = "무지개", width = 4.0)),
            ),
        )

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 임의의 색을 두르는 대신 겹을 만들지 않는다
        assertTrue(
            viewModel.state.value.toppings
                .first()
                .borderLayers
                .isEmpty(),
        )
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
        border: ToppingBorder = ToppingBorder.None,
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
        border = border,
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
