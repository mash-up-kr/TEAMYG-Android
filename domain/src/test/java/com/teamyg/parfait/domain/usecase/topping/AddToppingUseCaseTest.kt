package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddToppingUseCaseTest {
    private val imageUploadRepository: ImageUploadRepository = mockk()
    private val toppingRepository: ToppingRepository = mockk()
    private val addTopping = AddToppingUseCase(
        imageUploadRepository = imageUploadRepository,
        toppingRepository = toppingRepository,
    )

    private val transform = ToppingTransform(
        positionX = 0.5,
        positionY = 0.25,
        positionZ = 3,
        scale = 1.2,
        rotation = 15.0,
    )

    private val border = ToppingBorder.Solid(color = "#FFFF6B6B", width = 4.0)

    private val placed = PlacedToppingVO(
        parfaitImageId = ParfaitImageId(42L),
        imageId = CONFIRMED_IMAGE_ID,
        imageUrl = "https://cdn.example.com/nukki.png",
        transform = transform,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(10L),
            nickname = GroupNickname("연경이"),
            isMine = true,
        ),
    )

    private fun givenBothStepsSucceed() {
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.success(CONFIRMED_IMAGE_ID)
        coEvery {
            toppingRepository.place(any(), any(), any(), any(), any())
        } returns Result.success(placed)
    }

    // 프로퍼티 addTopping 과 이름을 나눠 둔다. 겹치면 호출부가 어느 쪽을 부르는지 읽기 어렵다
    private suspend fun addToppingWithFixtures() = addTopping(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        filePath = FILE_PATH,
        transform = transform,
        border = border,
    )

    @Test
    fun invoke_bothStepsSucceed_returnsPlacedTopping() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 배치 결과가 그대로 나온다
        assertEquals(placed, result.getOrThrow())
    }

    @Test
    fun invoke_bothStepsSucceed_placesTheConfirmedImageId() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val placedImageId = slot<ImageId>()
        coEvery {
            toppingRepository.place(any(), any(), capture(placedImageId), any(), any())
        } returns Result.success(placed)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 업로드가 돌려준 id 로 배치한다 — 두 단계를 잇는 유일한 값이다
        assertEquals(CONFIRMED_IMAGE_ID, placedImageId.captured)
    }

    @Test
    fun invoke_bothStepsSucceed_forwardsEveryArgumentVerbatim() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val sentFilePath = slot<String>()
        val sentTransform = slot<ToppingTransform>()
        val sentBorder = slot<ToppingBorder>()
        coEvery {
            imageUploadRepository.upload(capture(sentFilePath), any())
        } returns Result.success(CONFIRMED_IMAGE_ID)
        coEvery {
            toppingRepository.place(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                imageId = any(),
                transform = capture(sentTransform),
                border = capture(sentBorder),
            )
        } returns Result.success(placed)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 캔버스 식별값·경로·좌표·테두리가 손대지 않은 채 그대로 나간다. 이 층이 하는 일은
        // 순서를 정하는 것뿐인데, 값을 지어내면 서버는 200 을 주고 엉뚱한 자리에 토핑이 앉는다
        assertEquals(FILE_PATH, sentFilePath.captured)
        assertEquals(transform, sentTransform.captured)
        assertEquals(border, sentBorder.captured)
    }

    @Test
    fun invoke_bothStepsSucceed_uploadsAsNukki() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val uploadedType = slot<ImageType>()
        coEvery {
            imageUploadRepository.upload(any(), capture(uploadedType))
        } returns Result.success(CONFIRMED_IMAGE_ID)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 용도는 호출부가 고르지 않는다. BACKGROUND 로 올라가면 객체가 엉뚱한 S3 접두사에
        // 앉는데 배치는 그것을 검사하지 않아 아무 실패도 드러나지 않는다
        assertEquals(ImageType.NUKKI, uploadedType.captured)
    }

    @Test
    fun invoke_uploadFails_doesNotPlace() = runTest {
        // Given 업로드가 실패한다
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.failure(
            AppError.Network(cause = null),
        )

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 배치를 부르지 않는다 — 올라가지 않은 이미지의 id 가 없다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { toppingRepository.place(any(), any(), any(), any(), any()) }
    }

    @Test
    fun invoke_placeFails_propagatesErrorUnchanged() = runTest {
        // Given 업로드는 되고 마감된 파르페라 배치가 거절된다
        givenBothStepsSucceed()
        coEvery {
            toppingRepository.place(any(), any(), any(), any(), any())
        } returns Result.failure(
            AppError.Server(
                code = "PARFAIT_ALREADY_CLOSED",
                statusCode = 409,
                serverMessage = "이미 마감된 파르페입니다",
            ),
        )

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 코드가 살아서 올라온다. 화면이 이 코드로 되감기를 판정한다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_ALREADY_CLOSED", error.code)
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val CONFIRMED_IMAGE_ID = ImageId(99L)
        const val FILE_PATH = "/data/user/0/com.teamyg.parfait/cache/segmentation/subject.png"
    }
}
