package com.teamyg.parfait.data.source.group.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupDetailResponse
import com.teamyg.parfait.data.service.model.response.group.MyParfaitGroupResponse
import com.teamyg.parfait.data.service.model.response.group.ParfaitGroupMemberResponse
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

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

    private fun groupResponse(
        lastPlacedByNametagChip: String?,
        recentImageUploadedAt: String? = null,
    ) = MyParfaitGroupResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        recentImageUrl = null,
        recentImageUploadedAt = recentImageUploadedAt,
        lastPlacedByNametagChip = lastPlacedByNametagChip,
    )

    private fun detailResponse(memberChip: String?) = MyParfaitGroupDetailResponse(
        groupId = 1L,
        groupName = "모카의 파르페",
        groupNickname = "모카",
        inviteCode = "ABCDEF",
        memberLimit = 12,
        members = listOf(
            ParfaitGroupMemberResponse(
                memberId = 42L,
                groupNickname = "모카",
                nametagChip = memberChip,
            ),
        ),
    )

    @Test
    fun getMyGroups_knownChipString_becomesThatType() = runTest {
        // Given 서버가 마지막 토퍼의 칩을 enum 이름 문자열로 준다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("TYPE7")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 도메인 enum 으로 바뀐다
        assertEquals(NametagChipType.TYPE7, result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_releasedChip_isKeptNotFolded() = runTest {
        // Given 마지막 토퍼가 그룹을 나가 서버가 반납 표식을 준다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("RELEASED")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then null 로 접지 않는다 — "나간 사람"과 "값이 없다"는 뜻이 다르다
        assertEquals(NametagChipType.RELEASED, result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_missingChip_isNull() = runTest {
        // Given 아직 아무도 토핑을 올리지 않아 칩이 없다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse(null)))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then null 그대로 둔다
        assertNull(result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_unknownChipString_foldsToNull() = runTest {
        // Given 서버가 앱이 모르는 값을 준다 — 열린 입력이다
        coEvery { parfaitGroupService.getParfaitGroups() } returns success(listOf(groupResponse("TYPE99")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 던지지 않고 null 로 접는다 — 모르는 색은 그리지 못할 뿐이다
        assertNull(result.getOrNull()?.single()?.lastPlacedByNametagChip)
    }

    @Test
    fun getMyGroups_offsetlessUploadedAt_isReadAsSeoulWallClock() = runTest {
        // Given 서버가 오프셋 없는 로컬 날짜시각을 준다 — 그 벽시계는 Asia/Seoul 기준이다
        coEvery { parfaitGroupService.getParfaitGroups() } returns
            success(listOf(groupResponse(null, recentImageUploadedAt = "2026-08-01T12:00:00")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then KST 정오는 UTC 오전 3시와 같은 시점이다
        assertEquals(
            Instant.parse("2026-08-01T03:00:00Z"),
            result.getOrNull()?.single()?.recentImageUploadedAt,
        )
    }

    @Test
    fun getMyGroups_uploadedAtAcrossMidnight_staysOnItsOwnSeoulDay() = runTest {
        // Given 자정 직후 값이다 — UTC 로 읽으면 전날로 밀린다
        coEvery { parfaitGroupService.getParfaitGroups() } returns
            success(listOf(groupResponse(null, recentImageUploadedAt = "2026-08-02T00:30:00")))

        // When 목록을 받는다
        val result = dataSource.getMyGroups()

        // Then 서울 벽시계 8월 2일 00:30 = UTC 8월 1일 15:30
        assertEquals(
            Instant.parse("2026-08-01T15:30:00Z"),
            result.getOrNull()?.single()?.recentImageUploadedAt,
        )
    }

    @Test
    fun getGroupDetail_carriesNameLimitAndMemberChip() = runTest {
        // Given 서버가 그룹명·정원·멤버 칩을 함께 준다
        coEvery { parfaitGroupService.getParfaitGroupsByGroupId(1L) } returns success(detailResponse("TYPE3"))

        // When 상세를 받는다
        val detail = dataSource.getGroupDetail(GroupId(1L)).getOrNull()

        // Then 셋 다 VO 로 넘어온다 — 목록을 한 번 더 읽을 이유가 사라진다
        assertEquals("모카의 파르페", detail?.groupName?.value)
        assertEquals(12, detail?.memberLimit)
        assertEquals(NametagChipType.TYPE3, detail?.members?.single()?.nametagChip)
    }
}
