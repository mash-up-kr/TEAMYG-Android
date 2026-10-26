package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.topping.remote.ParfaitTextRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingAlign
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TextToppingRepositoryImplTest {
    private val parfaitTextRemoteDataSource: ParfaitTextRemoteDataSource = mockk()
    private val repository = TextToppingRepositoryImpl(parfaitTextRemoteDataSource)

    private val transform = ToppingTransform(
        positionX = 0.5,
        positionY = 0.25,
        positionZ = 3,
        scale = 1.2,
        rotation = 15.0,
    )

    private val style =
        TextToppingStyle(
            color = "#FFFF6B6B",
            fontSize = 24.0,
            isFill = true,
            isStroke = false,
            textAlign = TextToppingAlign.CENTER,
        )

    private val placed = PlacedTextToppingVO(
        parfaitTextId = PARFAIT_TEXT_ID,
        text = "안녕",
        transform = transform,
        style = style,
        placedBy = ToppingPlacerVO(groupMemberId = GroupMemberId(10L), nickname = GroupNickname("연경이")),
    )

    private val update = TextToppingUpdate(parfaitTextId = PARFAIT_TEXT_ID, scale = 2.0)

    private val updated = UpdatedTextToppingVO(
        parfaitTextId = PARFAIT_TEXT_ID,
        text = "안녕",
        transform = transform,
        style = style,
    )

    private val businessFailure = ApiException.Business(
        code = "PARFAIT_TEXT_NOT_OWNED",
        serverMessage = "본인이 배치한 텍스트가 아닙니다",
        statusCode = 403,
        errorDetail = null,
    )

    private suspend fun placeWithFixtures() = repository.place(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        text = "안녕",
        transform = transform,
        style = style,
    )

    @Test
    fun place_dataSourceSucceeds_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 배치 결과를 준다
        coEvery {
            parfaitTextRemoteDataSource.placeText(GROUP_ID, PARFAIT_ID, "안녕", transform, style)
        } returns Result.success(placed)

        // When 배치한다
        val result = placeWithFixtures()

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(placed, result.getOrThrow())
    }

    @Test
    fun place_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 서버가 비즈니스 오류를 준다
        coEvery {
            parfaitTextRemoteDataSource.placeText(any(), any(), any(), any(), any())
        } returns Result.failure(businessFailure)

        // When 배치한다
        val result = placeWithFixtures()

        // Then 코드와 상태 코드가 함께 살아 있다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_TEXT_NOT_OWNED", error.code)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun place_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery {
            parfaitTextRemoteDataSource.placeText(any(), any(), any(), any(), any())
        } returns Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 배치한다
        val result = placeWithFixtures()

        // Then ApiException 이 도메인까지 새지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun update_dataSourceSucceeds_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 수정 결과를 준다
        coEvery {
            parfaitTextRemoteDataSource.updateText(GROUP_ID, PARFAIT_ID, update)
        } returns Result.success(updated)

        // When 수정한다
        val result = repository.update(groupId = GROUP_ID, parfaitId = PARFAIT_ID, update = update)

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(updated, result.getOrThrow())
    }

    @Test
    fun update_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 본인 배치가 아니다
        coEvery {
            parfaitTextRemoteDataSource.updateText(GROUP_ID, PARFAIT_ID, update)
        } returns Result.failure(businessFailure)

        // When 수정한다
        val result = repository.update(groupId = GROUP_ID, parfaitId = PARFAIT_ID, update = update)

        // Then 코드와 상태 코드가 함께 살아 있다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_TEXT_NOT_OWNED", error.code)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun update_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery {
            parfaitTextRemoteDataSource.updateText(GROUP_ID, PARFAIT_ID, update)
        } returns Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 수정한다
        val result = repository.update(groupId = GROUP_ID, parfaitId = PARFAIT_ID, update = update)

        // Then ApiException 이 도메인까지 새지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun delete_dataSourceSucceeds_returnsUnit() = runTest {
        // Given 서버가 성공을 준다
        coEvery {
            parfaitTextRemoteDataSource.deleteText(GROUP_ID, PARFAIT_ID, PARFAIT_TEXT_ID)
        } returns Result.success(Unit)

        // When 지운다
        val result = repository.delete(groupId = GROUP_ID, parfaitId = PARFAIT_ID, parfaitTextId = PARFAIT_TEXT_ID)

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun delete_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 본인 배치가 아니다
        coEvery {
            parfaitTextRemoteDataSource.deleteText(GROUP_ID, PARFAIT_ID, PARFAIT_TEXT_ID)
        } returns Result.failure(businessFailure)

        // When 지운다
        val result = repository.delete(groupId = GROUP_ID, parfaitId = PARFAIT_ID, parfaitTextId = PARFAIT_TEXT_ID)

        // Then 코드와 상태 코드가 함께 살아 있다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_TEXT_NOT_OWNED", error.code)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun delete_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery {
            parfaitTextRemoteDataSource.deleteText(GROUP_ID, PARFAIT_ID, PARFAIT_TEXT_ID)
        } returns Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 지운다
        val result = repository.delete(groupId = GROUP_ID, parfaitId = PARFAIT_ID, parfaitTextId = PARFAIT_TEXT_ID)

        // Then ApiException 이 도메인까지 새지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val PARFAIT_TEXT_ID = ParfaitTextId(42L)
    }
}
