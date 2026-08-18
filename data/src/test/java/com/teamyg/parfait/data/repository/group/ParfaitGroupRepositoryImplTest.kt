package com.teamyg.parfait.data.repository.group

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSourceImpl
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.id.ReportId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitGroupRepositoryImplTest {
    private val remoteDataSource: ParfaitGroupRemoteDataSource = mockk()
    private val localDataSource = GroupLocalDataSourceImpl()
    private val repository = ParfaitGroupRepositoryImpl(remoteDataSource, localDataSource)

    private val inviteCode = InviteCode("ABCDEF")
    private val groupId = GroupId(1L)
    private val groupName = GroupName("모카의 파르페")
    private val groupNickname = GroupNickname("모카")

    @Test
    fun refreshMyGroups_succeeds_fillsCache() = runTest {
        // Given 서버가 그룹 하나를 준다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))

        // When 갱신한다
        val result = repository.refreshMyGroups()

        // Then 성공이고 캐시가 찼다
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
    }

    @Test
    fun refreshMyGroups_fails_keepsCacheAndMapsError() = runTest {
        // Given 캐시에 이미 목록이 있고, 다음 조회가 실패한다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))
        repository.refreshMyGroups()
        coEvery { remoteDataSource.getMyGroups() } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 다시 갱신한다
        val result = repository.refreshMyGroups()

        // Then 실패는 AppError 로 나오고 캐시는 그대로다
        assertIs<AppError.Network>(result.exceptionOrNull())
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
    }

    @Test
    fun refreshGroupDetail_fails_keepsCacheAndMapsError() = runTest {
        // Given 캐시에 이미 상세가 있고, 다음 조회가 실패한다
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)
        repository.refreshGroupDetail(GROUP_ID_A)
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 다시 갱신한다
        val result = repository.refreshGroupDetail(GROUP_ID_A)

        // Then 실패는 AppError 로 나오고 캐시는 그대로다
        assertIs<AppError.Network>(result.exceptionOrNull())
        assertEquals(DETAIL_A, repository.groupDetail(GROUP_ID_A).first())
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
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(emptyList())

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
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(emptyList())

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
    fun createGroup_succeeds_refreshesList() = runTest {
        // Given 생성이 성공하고 목록 재조회도 성공한다
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.success(CREATED)
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))

        // When 그룹을 만든다
        val result = repository.createGroup(GroupName("아메리카노"), GroupNickname("모카"), 12)

        // Then 생성 결과가 돌아오고 목록 캐시가 갱신된다
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_A), repository.myGroups.first())
        coVerify(exactly = 1) { remoteDataSource.getMyGroups() }
    }

    @Test
    fun createGroup_refreshFails_stillSucceeds() = runTest {
        // Given 생성은 성공했는데 뒤이은 목록 재조회가 실패한다
        coEvery { remoteDataSource.createGroup(any(), any(), any()) } returns Result.success(CREATED)
        coEvery { remoteDataSource.getMyGroups() } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 그룹을 만든다
        val result = repository.createGroup(GroupName("아메리카노"), GroupNickname("모카"), 12)

        // Then 이미 성공한 생성을 뒷정리 실패로 되돌리지 않는다
        assertTrue(result.isSuccess)
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
        coEvery { remoteDataSource.getGroupDetail(groupId) } returns Result.success(DETAIL_A)

        // When 닉네임 변경
        val result = repository.changeMyNickname(groupId = groupId, groupNickname = groupNickname)

        // Then 입력값이 가공 없이 전달되고 결과를 그대로 돌려준다
        assertEquals(nicknameVO, result.getOrThrow())
        coVerify(exactly = 1) {
            remoteDataSource.changeMyNickname(groupId = groupId, groupNickname = groupNickname)
        }
    }

    @Test
    fun changeMyNickname_succeeds_refreshFails_stillSucceeds() = runTest {
        // Given 닉네임 변경은 성공했는데 뒤이은 상세 재조회가 실패한다
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.success(NICKNAME_VO)
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns
            Result.failure(ApiException.Network(cause = IOException("no network")))

        // When 닉네임을 바꾼다
        val result = repository.changeMyNickname(GROUP_ID_A, GroupNickname("모카"))

        // Then 이미 성공한 변경을 뒷정리 실패로 되돌리지 않는다
        assertTrue(result.isSuccess)
    }

    @Test
    fun changeMyNickname_succeeds_refreshesDetail() = runTest {
        // Given 닉네임 변경과 상세 재조회가 모두 성공한다
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.success(NICKNAME_VO)
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)

        // When 닉네임을 바꾼다
        val result = repository.changeMyNickname(GROUP_ID_A, GroupNickname("모카"))

        // Then 상세 캐시가 서버 값으로 채워진다
        assertTrue(result.isSuccess)
        assertEquals(DETAIL_A, repository.groupDetail(GROUP_ID_A).first())
    }

    @Test
    fun changeMyNickname_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 서버 닉네임 규칙에 걸려 400 으로 실패
        coEvery { remoteDataSource.changeMyNickname(any(), any()) } returns Result.failure(
            ApiException.Business(
                code = "INVALID_GROUP_NICKNAME",
                serverMessage = "그룹 닉네임이 올바르지 않습니다",
                statusCode = 400,
                errorDetail = null,
            ),
        )

        // When 닉네임 변경
        val result = repository.changeMyNickname(groupId = groupId, groupNickname = groupNickname)

        // Then 도메인 에러로 바뀌어 나온다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("INVALID_GROUP_NICKNAME", error.code)
        assertEquals(400, error.statusCode)
    }

    @Test
    fun leaveGroup_remoteSucceeds_returnsLeftGroupId() = runTest {
        // Given 탈퇴에 성공한다
        coEvery { remoteDataSource.leaveGroup(any()) } returns Result.success(groupId)

        // When 그룹 나가기
        val result = repository.leaveGroup(groupId)

        // Then 탈퇴한 그룹 id 를 그대로 돌려준다
        assertEquals(groupId, result.getOrThrow())
        coVerify(exactly = 1) { remoteDataSource.leaveGroup(groupId) }
    }

    @Test
    fun leaveGroup_remoteFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 이미 나간 그룹이라 403 으로 실패
        coEvery { remoteDataSource.leaveGroup(any()) } returns Result.failure(
            ApiException.Business(
                code = "GROUP_NOT_JOINED",
                serverMessage = "참여하지 않은 그룹입니다",
                statusCode = 403,
                errorDetail = null,
            ),
        )

        // When 그룹 나가기
        val result = repository.leaveGroup(groupId)

        // Then 도메인 에러로 바뀌어 나온다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
        assertEquals(403, error.statusCode)
    }

    @Test
    fun leaveGroup_succeeds_removesFromCache() = runTest {
        // Given 목록·상세가 캐시에 있고 나가기가 성공한다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A, GROUP_B))
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)
        repository.refreshMyGroups()
        repository.refreshGroupDetail(GROUP_ID_A)
        coEvery { remoteDataSource.leaveGroup(GROUP_ID_A) } returns Result.success(GROUP_ID_A)

        // When 그룹에서 나간다
        val result = repository.leaveGroup(GROUP_ID_A)

        // Then 목록에서 빠지고 상세도 폐기된다. 재조회는 하지 않는다(403 뿐이다)
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_B), repository.myGroups.first())
        assertNull(repository.groupDetail(GROUP_ID_A).first())
        coVerify(exactly = 1) { remoteDataSource.getMyGroups() }
    }

    @Test
    fun reportGroup_passesGroupIdAndReasonToDataSource() = runTest {
        // Given 신고 접수에 성공한다
        val reportedGroup = ReportedGroupVO(groupId = groupId, reportId = ReportId(9L))
        coEvery { remoteDataSource.reportGroup(any(), any()) } returns Result.success(reportedGroup)

        // When 그룹 신고
        val result = repository.reportGroup(groupId = groupId, reason = REPORT_REASON)

        // Then 사유를 가공하지 않고 넘기고 결과를 그대로 돌려준다
        assertEquals(reportedGroup, result.getOrThrow())
        coVerify(exactly = 1) { remoteDataSource.reportGroup(groupId = groupId, reason = REPORT_REASON) }
    }

    @Test
    fun reportGroup_remoteFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery { remoteDataSource.reportGroup(any(), any()) } returns Result.failure(
            ApiException.Network(cause = IOException("연결 실패")),
        )

        // When 그룹 신고
        val result = repository.reportGroup(groupId = groupId, reason = REPORT_REASON)

        // Then 도메인 에러로 바뀌어 나온다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    @Test
    fun reportGroup_succeeds_removesFromCache() = runTest {
        // Given 목록·상세가 캐시에 있고 신고가 성공한다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A, GROUP_B))
        coEvery { remoteDataSource.getGroupDetail(GROUP_ID_A) } returns Result.success(DETAIL_A)
        repository.refreshMyGroups()
        repository.refreshGroupDetail(GROUP_ID_A)
        coEvery { remoteDataSource.reportGroup(GROUP_ID_A, any()) } returns
            Result.success(ReportedGroupVO(groupId = GROUP_ID_A, reportId = ReportId(9L)))

        // When 그룹을 신고한다
        val result = repository.reportGroup(groupId = GROUP_ID_A, reason = REPORT_REASON)

        // Then 목록에서 빠지고 상세도 폐기된다. 재조회는 하지 않는다(신고는 탈퇴로 이어져
        // 그 뒤로는 403 뿐이다)
        assertTrue(result.isSuccess)
        assertEquals(listOf(GROUP_B), repository.myGroups.first())
        assertNull(repository.groupDetail(GROUP_ID_A).first())
        coVerify(exactly = 1) { remoteDataSource.getMyGroups() }
    }

    @Test
    fun clearGroups_emptiesCache() = runTest {
        // Given 캐시가 차 있다
        coEvery { remoteDataSource.getMyGroups() } returns Result.success(listOf(GROUP_A))
        repository.refreshMyGroups()

        // When 세션이 끝난다
        repository.clearGroups()

        // Then 미조회로 되돌아간다
        assertNull(repository.myGroups.first())
    }

    private companion object {
        const val REPORT_REASON = "부적절한 그룹"

        val GROUP_ID_A = GroupId(1L)
        val GROUP_ID_B = GroupId(2L)

        val GROUP_A = MyParfaitGroupVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("아메리카노"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = null,
        )
        val GROUP_B = MyParfaitGroupVO(
            groupId = GROUP_ID_B,
            groupName = GroupName("라떼"),
            recentImageUrl = null,
            recentImageUploadedAt = null,
            lastPlacedByNametagChip = null,
        )

        val DETAIL_A = ParfaitGroupDetailVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("모카의 파르페"),
            groupNickname = GroupNickname("모카"),
            inviteCode = InviteCode("ABC123"),
            memberLimit = 12,
            members = listOf(
                ParfaitGroupMemberVO(
                    memberId = MemberId(10L),
                    groupNickname = GroupNickname("모카"),
                    nametagChip = NametagChipType.TYPE1,
                ),
            ),
        )

        val NICKNAME_VO = GroupNicknameVO(groupId = GROUP_ID_A, groupNickname = GroupNickname("모카"))
        val CREATED = CreatedGroupVO(
            groupId = GROUP_ID_A,
            groupName = GroupName("아메리카노"),
            inviteCode = InviteCode("ABC123"),
            memberLimit = 12,
        )
    }
}
