package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ToppingRepositoryImplTest {
    private val parfaitImageRemoteDataSource: ParfaitImageRemoteDataSource = mockk()
    private val repository = ToppingRepositoryImpl(parfaitImageRemoteDataSource)

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
        imageId = IMAGE_ID,
        imageUrl = "https://cdn.example.com/nukki.png",
        transform = transform,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(10L),
            nickname = GroupNickname("연경이"),
        ),
    )

    // SUT 의 place() 와 이름을 나눠 둔다. 겹치면 호출부가 어느 쪽을 부르는지 읽기 어렵다
    private suspend fun placeWithFixtures() = repository.place(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        imageId = IMAGE_ID,
        transform = transform,
        border = border,
    )

    @Test
    fun place_dataSourceSucceeds_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 배치 결과를 준다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.success(placed)

        // When 배치한다
        val result = placeWithFixtures()

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(placed, result.getOrThrow())
    }

    @Test
    fun place_onceCalled_forwardsEveryArgumentVerbatim() = runTest {
        // Given 원격 데이터소스가 배치 결과를 준다
        val sentTransform = slot<ToppingTransform>()
        val sentBorder = slot<ToppingBorder>()
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                imageId = IMAGE_ID,
                transform = capture(sentTransform),
                border = capture(sentBorder),
            )
        } returns Result.success(placed)

        // When 배치한다
        placeWithFixtures()

        // Then 좌표와 테두리가 손대지 않은 채 그대로 나간다 — 이 층은 에러 변환만 한다.
        // 테두리를 흘리면 서버는 200 을 주고 캔버스에서 테두리만 조용히 사라진다
        coVerify(exactly = 1) {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        }
        assertEquals(transform, sentTransform.captured)
        assertEquals(border, sentBorder.captured)
    }

    @Test
    fun place_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 마감된 파르페에 올리려 한다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.failure(
            ApiException.Business(
                code = "PARFAIT_ALREADY_CLOSED",
                serverMessage = "이미 마감된 파르페입니다",
                statusCode = 409,
                errorDetail = null,
            ),
        )

        // When 배치한다
        val result = placeWithFixtures()

        // Then 코드와 상태 코드가 함께 살아 있다 — 화면이 둘을 같이 봐야 되감기를 판정한다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_ALREADY_CLOSED", error.code)
        assertEquals(409, error.statusCode)
    }

    @Test
    fun place_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 배치한다
        val result = placeWithFixtures()

        // Then ApiException 이 도메인까지 새지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val IMAGE_ID = ImageId(3L)
    }
}
