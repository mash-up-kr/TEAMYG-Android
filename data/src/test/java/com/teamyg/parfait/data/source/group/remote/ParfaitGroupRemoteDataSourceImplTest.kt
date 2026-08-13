package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.model.request.group.CreateParfaitGroupRequest
import com.teamyg.parfait.data.service.model.request.group.JoinParfaitGroupRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.group.CreateParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.JoinParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.PreviewParfaitGroupJoinResponse
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.id.GroupId
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
import kotlin.test.assertTrue

class ParfaitGroupRemoteDataSourceImplTest {
    private val parfaitGroupService: ParfaitGroupService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitGroupRemoteDataSourceImpl(
        parfaitGroupService = parfaitGroupService,
        apiCaller = apiCaller,
    )

    private fun <T> success(data: T) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = data,
    )

    @Test
    fun previewJoin_serviceReturnsSuccess_returnsGroupName() = runTest {
        // Given 초대코드로 조회한 그룹명이 내려온다
        coEvery { parfaitGroupService.getParfaitGroupsJoinPreview(any()) } returns
            success(PreviewParfaitGroupJoinResponse(groupName = "모카의 파르페"))

        // When 참여 미리보기 조회
        val result = dataSource.previewJoin(InviteCode("WDIDCJ"))

        // Then 그룹명 VO 로 매핑된다
        assertEquals(GroupName("모카의 파르페"), result.getOrThrow())
    }

    @Test
    fun previewJoin_passesRawInviteCodeToService() = runTest {
        // Given 성공 응답
        val inviteCode = slot<String>()
        coEvery { parfaitGroupService.getParfaitGroupsJoinPreview(capture(inviteCode)) } returns
            success(PreviewParfaitGroupJoinResponse(groupName = "모카의 파르페"))

        // When 참여 미리보기 조회
        dataSource.previewJoin(InviteCode("WDIDCJ"))

        // Then value class 가 아니라 문자열 값이 그대로 전달된다
        assertEquals("WDIDCJ", inviteCode.captured)
        coVerify(exactly = 1) { parfaitGroupService.getParfaitGroupsJoinPreview(any()) }
    }

    @Test
    fun previewJoin_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 잘못된 초대코드라고 응답
        coEvery { parfaitGroupService.getParfaitGroupsJoinPreview(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_INVITE_CODE",
            message = "유효하지 않은 초대코드입니다",
            data = null,
        )

        // When 참여 미리보기 조회
        val result = dataSource.previewJoin(InviteCode("WDIDCJ"))

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_INVITE_CODE", error.code)
        assertEquals("유효하지 않은 초대코드입니다", error.serverMessage)
    }

    @Test
    fun previewJoin_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { parfaitGroupService.getParfaitGroupsJoinPreview(any()) } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 참여 미리보기 조회
        val result = dataSource.previewJoin(InviteCode("WDIDCJ"))

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
    }

    @Test
    fun previewJoin_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { parfaitGroupService.getParfaitGroupsJoinPreview(any()) } throws IOException("connection reset")

        // When 참여 미리보기 조회
        val result = dataSource.previewJoin(InviteCode("WDIDCJ"))

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun joinGroup_serviceReturnsSuccess_returnsJoinedGroupVo() = runTest {
        // Given 참여한 그룹 정보가 내려온다
        coEvery { parfaitGroupService.postParfaitGroupsJoin(any()) } returns
            success(JoinParfaitGroupResponse(groupId = 12L, groupName = "모카의 파르페"))

        // When 그룹 참여
        val vo = dataSource.joinGroup(InviteCode("WDIDCJ")).getOrThrow()

        // Then 모든 필드가 제자리에 매핑된다
        assertEquals(GroupId(12L), vo.groupId)
        assertEquals(GroupName("모카의 파르페"), vo.groupName)
    }

    @Test
    fun joinGroup_sendsInviteCodeInRequestBody() = runTest {
        // Given 성공 응답
        val request = slot<JoinParfaitGroupRequest>()
        coEvery { parfaitGroupService.postParfaitGroupsJoin(capture(request)) } returns
            success(JoinParfaitGroupResponse(groupId = 12L, groupName = "모카의 파르페"))

        // When 그룹 참여
        dataSource.joinGroup(InviteCode("WDIDCJ"))

        // Then 요청 바디에 초대코드가 담긴다
        assertEquals("WDIDCJ", request.captured.inviteCode)
    }

    @Test
    fun joinGroup_unexpectedException_returnsUnknownException() = runTest {
        // Given 예상 못 한 예외
        coEvery { parfaitGroupService.postParfaitGroupsJoin(any()) } throws IllegalStateException("boom")

        // When 그룹 참여
        val result = dataSource.joinGroup(InviteCode("WDIDCJ"))

        // Then Unknown 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }

    @Test
    fun createGroup_serviceReturnsSuccess_mapsEveryField() = runTest {
        // Given 생성된 그룹 정보가 내려온다
        coEvery { parfaitGroupService.postParfaitGroups(any()) } returns success(
            CreateParfaitGroupResponse(
                groupId = 7L,
                groupName = "모카의 파르페",
                inviteCode = "WDIDCJ",
                memberLimit = 4,
            ),
        )

        // When 그룹 생성
        val result = dataSource.createGroup(
            groupName = GroupName("모카의 파르페"),
            groupNickname = GroupNickname("체리마루"),
            memberLimit = 4,
        )
        val vo = result.getOrThrow()

        // Then groupName 과 inviteCode 가 둘 다 String 이라 뒤바뀌어도 컴파일되므로 전 필드를 확인한다
        assertEquals(GroupId(7L), vo.groupId)
        assertEquals(GroupName("모카의 파르페"), vo.groupName)
        assertEquals(InviteCode("WDIDCJ"), vo.inviteCode)
        assertEquals(4, vo.memberLimit)
    }

    @Test
    fun createGroup_sendsNameNicknameAndMemberLimitInRequestBody() = runTest {
        // Given 성공 응답
        val request = slot<CreateParfaitGroupRequest>()
        coEvery { parfaitGroupService.postParfaitGroups(capture(request)) } returns success(
            CreateParfaitGroupResponse(
                groupId = 7L,
                groupName = "모카의 파르페",
                inviteCode = "WDIDCJ",
                memberLimit = 4,
            ),
        )

        // When 그룹 생성
        dataSource.createGroup(
            groupName = GroupName("모카의 파르페"),
            groupNickname = GroupNickname("체리마루"),
            memberLimit = 4,
        )

        // Then 요청 바디에 입력값이 그대로 담긴다
        assertEquals("모카의 파르페", request.captured.groupName)
        assertEquals("체리마루", request.captured.groupNickname)
        assertEquals(4, request.captured.memberLimit)
    }

    @Test
    fun createGroup_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 생성 제한으로 실패 응답
        coEvery { parfaitGroupService.postParfaitGroups(any()) } returns ApiResponse(
            success = false,
            code = "GROUP_LIMIT_EXCEEDED",
            message = "생성 가능한 그룹 수를 초과했습니다",
            data = null,
        )

        // When 그룹 생성
        val result = dataSource.createGroup(
            groupName = GroupName("모카의 파르페"),
            groupNickname = GroupNickname("체리마루"),
            memberLimit = 4,
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_LIMIT_EXCEEDED", error.code)
    }
}
