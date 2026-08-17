package com.teamyg.parfait.data.repository.group

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
class ParfaitGroupRepositoryImpl @Inject constructor(
    private val parfaitGroupRemoteDataSource: ParfaitGroupRemoteDataSource,
) : ParfaitGroupRepository {
    override suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>> = parfaitGroupRemoteDataSource
        .getMyGroups()
        .mapErrorToAppError()

    override suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO> = parfaitGroupRemoteDataSource
        .getGroupDetail(groupId)
        .mapErrorToAppError()

    override suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName> = parfaitGroupRemoteDataSource
        .previewJoin(inviteCode)
        .mapErrorToAppError()

    override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = parfaitGroupRemoteDataSource
        .joinGroup(inviteCode)
        .mapErrorToAppError()

    override suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> = parfaitGroupRemoteDataSource
        .createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = memberLimit,
        ).mapErrorToAppError()

    override suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = parfaitGroupRemoteDataSource
        .changeMyNickname(
            groupId = groupId,
            groupNickname = groupNickname,
        ).mapErrorToAppError()
}
