package com.teamyg.parfait.data.repository.group

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class ParfaitGroupRepositoryImplTest {
    private val remoteDataSource: ParfaitGroupRemoteDataSource = mockk()
    private val repository = ParfaitGroupRepositoryImpl(remoteDataSource)

    private val inviteCode = InviteCode("ABCDEF")
    private val groupId = GroupId(1L)
    private val groupName = GroupName("모카의 파르페")
    private val groupNickname = GroupNickname("모카")

    @Test
    fun getMyGroups_remoteSucceeds_returnsListUnchanged() = runTest {
        // Given 원격이 그룹 두 개를 준다
        val myGroups = listOf(
            MyParfaitGroupVO(
                groupId = groupId,
                groupName = groupName,
                recentImageUrl = "https://cdn.example.com/a.png",
                recentImageUploadedAt = Instant.parse("2026-08-15T10:00:00Z"),
            ),
            MyParfaitGroupVO(
                groupId = GroupId(2L),
                groupName = GroupName("우리집"),
                recentImageUrl = null,
                recentImageUploadedAt = null,
            ),
        )
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(myGroups)

        // When 내 그룹 목록 조회
        val result = repository.getMyGroups()

        // Then 서버가 준 정렬을 그대로 유지한 채 전달한다
        assertEquals(myGroups, result.getOrThrow())
    }

    @Test
    fun getMyGroups_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.getMyGroups() } returns
            Result.failure(ApiException.Network(cause = IOException("offline")))

        // When 내 그룹 목록 조회
        val result = repository.getMyGroups()

        // Then Network 갈래로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun previewJoin_remoteSucceeds_returnsGroupNameUnchanged() = runTest {
        // Given 원격이 그룹명을 준다
        coEvery { remoteDataSource.previewJoin(inviteCode) } returns Result.success(groupName)

        // When 참여 미리보기
        val result = repository.previewJoin(inviteCode)

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(groupName, result.getOrThrow())
    }

    @Test
    fun previewJoin_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 초대코드가 없는 그룹을 가리켜 404 로 실패
        coEvery { remoteDataSource.previewJoin(any()) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_INVITE_CODE",
                serverMessage = "유효하지 않은 초대코드입니다",
                statusCode = 404,
                errorDetail = null,
            ),
        )

        // When 참여 미리보기
        val result = repository.previewJoin(inviteCode)

        // Then 도메인 에러로 바뀌어 나온다(호출부가 :data 를 보지 않아도 된다)
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_INVITE_CODE", error.code)
        assertEquals(404, error.statusCode)
    }

    @Test
    fun joinGroup_remoteSucceeds_returnsJoinedGroup() = runTest {
        // Given 참여에 성공한다
        val joinedGroup = JoinedGroupVO(groupId = groupId, groupName = groupName)
        coEvery { remoteDataSource.joinGroup(any()) } returns Result.success(joinedGroup)

        // When 그룹 참여
        val result = repository.joinGroup(inviteCode)

        // Then 참여한 그룹을 그대로 돌려주고 초대코드가 가공 없이 전달된다
        assertEquals(joinedGroup, result.getOrThrow())
        coVerify(exactly = 1) { remoteDataSource.joinGroup(inviteCode) }
    }

    @Test
    fun joinGroup_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결 실패
        coEvery { remoteDataSource.joinGroup(any()) } returns
            Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 그룹 참여
        val result = repository.joinGroup(inviteCode)

        // Then Network 갈래로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun createGroup_remoteSucceeds_returnsVoUnchangedAndDelegatesArguments() = runTest {
        // Given 원격이 생성된 그룹으로 응답
        val createdGroup = CreatedGroupVO(
            groupId = groupId,
            groupName = groupName,
            inviteCode = inviteCode,
            memberLimit = 6,
        )
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.success(createdGroup)

        // When 그룹 생성
        val result = repository.createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = 6,
        )

        // Then VO 가 그대로 나오고 인자도 그대로 넘어간다
        assertEquals(createdGroup, result.getOrThrow())
        coVerify(exactly = 1) {
            remoteDataSource.createGroup(
                groupName = groupName,
                groupNickname = groupNickname,
                memberLimit = 6,
            )
        }
    }

    @Test
    fun createGroup_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 정원이 범위를 벗어나 400 으로 실패
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_GROUP_MEMBER_LIMIT",
                serverMessage = "그룹 최대 인원은 1명 이상 12명 이하여야 합니다",
                statusCode = 400,
                errorDetail = null,
            ),
        )

        // When 그룹 생성
        val result = repository.createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = 99,
        )

        // Then 도메인 에러로 바뀌어 나온다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_GROUP_MEMBER_LIMIT", error.code)
        assertEquals(400, error.statusCode)
    }

    @Test
    fun createGroup_remoteFailsWithEmptyBody_convertsToAppErrorUnexpected() = runTest {
        // Given envelope 는 성공인데 본문이 비어 있다
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns
            Result.failure(ApiException.EmptyBody("SUCCESS", "성공"))

        // When 그룹 생성
        val result = repository.createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = 6,
        )

        // Then Unexpected 갈래로 떨어진다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }

    @Test
    fun changeMyNickname_passesGroupIdAndNicknameToDataSource() = runTest {
        // Given 닉네임 변경에 성공한다
        val nicknameVO = GroupNicknameVO(groupId = groupId, groupNickname = groupNickname)
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.success(nicknameVO)

        // When 닉네임 변경
        val result = repository.changeMyNickname(groupId = groupId, groupNickname = groupNickname)

        // Then 입력값이 가공 없이 전달되고 결과를 그대로 돌려준다
        assertEquals(nicknameVO, result.getOrThrow())
        coVerify(exactly = 1) {
            remoteDataSource.changeMyNickname(groupId = groupId, groupNickname = groupNickname)
        }
    }

    @Test
    fun changeMyNickname_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 같은 그룹에서 이미 쓰이는 닉네임이라 409 로 실패
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.failure(
            ApiException.Business(
                code = "GROUP_NICKNAME_ALREADY_USED",
                serverMessage = "이미 사용 중인 닉네임입니다",
                statusCode = 409,
                errorDetail = null,
            ),
        )

        // When 닉네임 변경
        val result = repository.changeMyNickname(groupId = groupId, groupNickname = groupNickname)

        // Then 도메인 에러로 바뀌어 나온다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("GROUP_NICKNAME_ALREADY_USED", error.code)
        assertEquals(409, error.statusCode)
    }
}
