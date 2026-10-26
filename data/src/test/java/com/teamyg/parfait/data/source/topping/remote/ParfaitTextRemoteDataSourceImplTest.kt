package com.teamyg.parfait.data.source.topping.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitTextService
import com.teamyg.parfait.data.service.model.request.parfaittext.PlaceParfaitTextRequest
import com.teamyg.parfait.data.service.model.request.parfaittext.UpdateParfaitTextRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfait.PlacedByResponse
import com.teamyg.parfait.data.service.model.response.parfait.TodayParfaitTextResponse
import com.teamyg.parfait.data.service.model.response.parfaittext.UpdateParfaitTextResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitTextRemoteDataSourceImplTest {
    private val parfaitTextService: ParfaitTextService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitTextRemoteDataSourceImpl(
        parfaitTextService = parfaitTextService,
        apiCaller = apiCaller,
    )

    private val transform = ToppingTransform(
        positionX = 120.5,
        positionY = 340.2,
        positionZ = 3,
        scale = 1.5,
        rotation = 15.0,
    )

    private val style = TextToppingStyle(
        color = "#FFFFFF",
        fontSize = 16.0,
        isFill = true,
        isStroke = false,
    )

    private fun placeSuccess() = ApiResponse(
        success = true,
        code = "CREATED",
        message = "성공",
        data = TodayParfaitTextResponse(
            parfaitTextId = 301L,
            text = "오늘도 화이팅",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 3,
            scale = 1.5,
            rotation = 15.0,
            color = "#FFFFFF",
            fontSize = 16.0,
            isFill = true,
            isStroke = false,
            placedBy = PlacedByResponse(groupMemberId = 10L, nickname = "연경이", ownerType = "ME"),
            createdAt = "2026-10-25T10:00:00.123",
        ),
    )

    private suspend fun place() = dataSource.placeText(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(5L),
        text = "오늘도 화이팅",
        transform = transform,
        style = style,
    )

    @Test
    fun placeText_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 배치 성공 응답을 준다
        coEvery {
            parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(any(), any(), any())
        } returns placeSuccess()

        // When 텍스트 배치
        val vo = place().getOrThrow()

        // Then 식별자·내용·transform·글자 모양·배치자가 제자리에 들어간다
        assertEquals(ParfaitTextId(301L), vo.parfaitTextId)
        assertEquals("오늘도 화이팅", vo.text)
        assertEquals(transform, vo.transform)
        assertEquals(style, vo.style)
        assertEquals(GroupMemberId(10L), vo.placedBy.groupMemberId)
        assertEquals(GroupNickname("연경이"), vo.placedBy.nickname)
    }

    @Test
    fun placeText_buildsRequestBodyAndUnwrapsPathIds() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitTextRequest>()
        coEvery {
            parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(any(), any(), capture(request))
        } returns placeSuccess()

        // When 텍스트 배치
        place()

        // Then 경로는 원시 id, 바디는 transform·style 을 평면으로 편 값이다
        coVerify { parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(1L, 5L, any()) }
        assertEquals(
            PlaceParfaitTextRequest(
                text = "오늘도 화이팅",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 3,
                scale = 1.5,
                rotation = 15.0,
                color = "#FFFFFF",
                fontSize = 16.0,
                isFill = true,
                isStroke = false,
            ),
            request.captured,
        )
    }

    @Test
    fun placeText_parfaitClosed_returnsBusinessException() = runTest {
        // Given 마감된 캔버스
        coEvery {
            parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_ALREADY_CLOSED",
            message = "이미 마감된 파르페입니다",
            data = null,
        )

        // When 텍스트 배치
        val result = place()

        // Then Business 예외로 실패한다
        assertEquals("PARFAIT_ALREADY_CLOSED", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }

    @Test
    fun placeText_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(any(), any(), any())
        } throws IOException("connection reset")

        // When 텍스트 배치
        val result = place()

        // Then Network 예외로 감싸진다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    private fun updateSuccess() = ApiResponse(
        success = true,
        code = "OK",
        message = "성공",
        data = UpdateParfaitTextResponse(
            parfaitTextId = 301L,
            text = "오늘도 화이팅",
            positionX = 200.0,
            positionY = 340.2,
            positionZ = 3,
            scale = 1.5,
            rotation = 15.0,
            color = "#000000",
            fontSize = 16.0,
            isFill = true,
            isStroke = false,
        ),
    )

    @Test
    fun updateText_serviceReturnsSuccess_returnsMergedVo() = runTest {
        // Given 서버가 병합한 전체 값을 준다
        coEvery {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(any(), any(), any(), any())
        } returns updateSuccess()

        // When 일부 필드만 수정
        val vo = dataSource
            .updateText(
                groupId = GroupId(1L),
                parfaitId = ParfaitId(5L),
                update = TextToppingUpdate(parfaitTextId = ParfaitTextId(301L), positionX = 200.0, color = "#000000"),
            ).getOrThrow()

        // Then 요청과 무관하게 응답의 전체 값이 VO 로 들어간다
        assertEquals(ParfaitTextId(301L), vo.parfaitTextId)
        assertEquals("오늘도 화이팅", vo.text)
        assertEquals(
            ToppingTransform(positionX = 200.0, positionY = 340.2, positionZ = 3, scale = 1.5, rotation = 15.0),
            vo.transform,
        )
        assertEquals(
            TextToppingStyle(color = "#000000", fontSize = 16.0, isFill = true, isStroke = false),
            vo.style,
        )
    }

    @Test
    fun updateText_allFieldsSet_buildsRequestBodyFieldByField() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitTextRequest>()
        coEvery {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
                any(),
                any(),
                any(),
                capture(request),
            )
        } returns updateSuccess()

        // When 아홉 필드를 모두 서로 다른 값으로 바꾼다
        dataSource.updateText(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            update = TextToppingUpdate(
                parfaitTextId = ParfaitTextId(301L),
                positionX = 1.0,
                positionY = 2.0,
                positionZ = 3,
                scale = 4.0,
                rotation = 5.0,
                color = "#123456",
                fontSize = 6.0,
                isFill = true,
                isStroke = false,
            ),
        )

        // Then 각 값이 같은 이름의 필드로 간다
        assertEquals(
            UpdateParfaitTextRequest(
                positionX = 1.0,
                positionY = 2.0,
                positionZ = 3,
                scale = 4.0,
                rotation = 5.0,
                color = "#123456",
                fontSize = 6.0,
                isFill = true,
                isStroke = false,
            ),
            request.captured,
        )
    }

    @Test
    fun updateText_omittedFieldsAreSentAsNullAndIdGoesToPath() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitTextRequest>()
        coEvery {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
                any(),
                any(),
                any(),
                capture(request),
            )
        } returns updateSuccess()

        // When isStroke 만 바꾼다
        dataSource.updateText(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            update = TextToppingUpdate(parfaitTextId = ParfaitTextId(301L), isStroke = true),
        )

        // Then 텍스트 id 는 경로로, 안 바꾸는 필드는 null 로 나간다
        coVerify {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(1L, 5L, 301L, any())
        }
        assertEquals(true, request.captured.isStroke)
        assertNull(request.captured.positionX)
        assertNull(request.captured.color)
        assertNull(request.captured.isFill)
    }

    @Test
    fun updateText_notOwned_returnsBusinessException() = runTest {
        // Given 남의 텍스트
        coEvery {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(any(), any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_TEXT_NOT_OWNED",
            message = "본인이 배치한 텍스트가 아닙니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateText(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            update = TextToppingUpdate(parfaitTextId = ParfaitTextId(301L)),
        )

        // Then Business 예외로 실패한다
        assertEquals("PARFAIT_TEXT_NOT_OWNED", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }

    @Test
    fun deleteText_serviceReturnsSuccess_returnsUnit() = runTest {
        // Given 서버가 200 과 빈 data 를 준다
        coEvery {
            parfaitTextService.deleteGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(1L, 2L, 3L)
        } returns ApiResponse(success = true, code = "OK", message = "성공", data = null)

        // When 텍스트를 지운다
        val result = dataSource.deleteText(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitTextId = ParfaitTextId(3L),
        )

        // Then data 가 없어도 성공이다
        assertTrue(result.isSuccess)
    }

    @Test
    fun deleteText_alreadyDeleted_returnsBusinessFailure() = runTest {
        // Given 이미 지운 텍스트를 다시 지운다(삭제는 멱등이 아니다)
        coEvery {
            parfaitTextService.deleteGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(1L, 2L, 3L)
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_TEXT_NOT_FOUND",
            message = "존재하지 않는 텍스트입니다",
            data = null,
        )

        // When 텍스트를 지운다
        val result = dataSource.deleteText(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(2L),
            parfaitTextId = ParfaitTextId(3L),
        )

        // Then Business 예외로 실패한다
        assertEquals("PARFAIT_TEXT_NOT_FOUND", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }
}
