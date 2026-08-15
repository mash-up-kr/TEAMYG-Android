package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateGroupUseCaseTest {
    private val parfaitGroupRepository: ParfaitGroupRepository = mockk()
    private val useCase = CreateGroupUseCase(parfaitGroupRepository)

    private fun createdGroup(groupId: Long) = CreatedGroupVO(
        groupId = GroupId(groupId),
        groupName = GroupName("파르페"),
        inviteCode = InviteCode("ABC123"),
        memberLimit = 6,
    )

    @Test
    fun invoke_passesArgumentsToRepositoryUnchanged() = runTest {
        // Given 그룹 생성이 성공하는 원격
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns
            Result.success(createdGroup(groupId = 1))

        // When 그룹 생성
        useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then 입력이 그대로 Repository 로 넘어간다
        coVerify(exactly = 1) {
            parfaitGroupRepository.createGroup(
                groupName = GroupName("파르페"),
                groupNickname = GroupNickname("체리"),
                memberLimit = 6,
            )
        }
    }

    @Test
    fun invoke_positiveGroupId_returnsCreatedGroup() = runTest {
        // Given 서버가 유효한 groupId 를 내려준다
        val vo = createdGroup(groupId = 42)
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns Result.success(vo)

        // When 그룹 생성
        val result = useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then VO 가 그대로 성공으로 나온다
        assertEquals(vo, result.getOrNull())
    }

    @Test
    fun invoke_zeroGroupId_returnsUnexpectedFailure() = runTest {
        // Given envelope 는 성공인데 groupId 가 0 이다(서버가 값을 못 채운 경우)
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns
            Result.success(createdGroup(groupId = 0))

        // When 그룹 생성
        val result = useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then 성공으로 통과시키지 않는다 — 0 인 ID 로 다음 요청이 나가면 안 된다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }

    @Test
    fun invoke_negativeGroupId_returnsUnexpectedFailure() = runTest {
        // Given groupId 가 음수다
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns
            Result.success(createdGroup(groupId = -1))

        // When 그룹 생성
        val result = useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then 실패다
        assertIs<AppError.Unexpected>(result.exceptionOrNull())
    }

    @Test
    fun invoke_repositoryFails_propagatesErrorUnchanged() = runTest {
        // Given 서버가 비즈니스 에러로 실패
        val error = AppError.Server(
            code = "GROUP_LIMIT_EXCEEDED",
            statusCode = 400,
            serverMessage = "…",
        )
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns Result.failure(error)

        // When 그룹 생성
        val result = useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then 에러를 덧씌우지 않고 그대로 전달한다 — 화면이 사유별로 분기할 수 있어야 한다
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun invoke_networkFails_propagatesNetworkError() = runTest {
        // Given 연결 실패
        coEvery { parfaitGroupRepository.createGroup(any(), any(), any()) } returns
            Result.failure(AppError.Network(null))

        // When 그룹 생성
        val result = useCase(
            groupName = GroupName("파르페"),
            groupNickname = GroupNickname("체리"),
            memberLimit = 6,
        )

        // Then Network 갈래가 유지된다(재시도를 권할 수 있는 유일한 갈래다)
        assertIs<AppError.Network>(result.exceptionOrNull())
    }
}
