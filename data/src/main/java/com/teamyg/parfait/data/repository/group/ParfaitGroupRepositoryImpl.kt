package com.teamyg.parfait.data.repository.group

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSource
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ReportedGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 *
 * 목록·상세는 [GroupLocalDataSource] 인메모리 캐시가 SSoT 다(ADR-0023) — 조회는 캐시를
 * 읽는 [Flow] 하나, 서버 재조회는 [refreshMyGroups]·[refreshGroupDetail] 로 갈라 둔다.
 */
class ParfaitGroupRepositoryImpl @Inject constructor(
    private val parfaitGroupRemoteDataSource: ParfaitGroupRemoteDataSource,
    private val groupLocalDataSource: GroupLocalDataSource,
) : ParfaitGroupRepository {
    override val myGroups: Flow<List<MyParfaitGroupVO>?> = groupLocalDataSource.myGroups

    override fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?> = groupLocalDataSource.groupDetail(groupId)

    override suspend fun refreshMyGroups(): Result<Unit> = parfaitGroupRemoteDataSource
        .getMyGroups()
        .onSuccess(groupLocalDataSource::saveMyGroups)
        .map { }
        .mapErrorToAppError()

    override suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit> = parfaitGroupRemoteDataSource
        .getGroupDetail(groupId)
        .onSuccess(groupLocalDataSource::saveGroupDetail)
        .map { }
        .mapErrorToAppError()

    override fun clearGroups() = groupLocalDataSource.clear()

    override suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName> = parfaitGroupRemoteDataSource
        .previewJoin(inviteCode)
        .mapErrorToAppError()

    /** 참여 응답도 목록 항목을 세울 수 없어 다시 받는다([createGroup] 과 같은 이유) */
    override suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO> = parfaitGroupRemoteDataSource
        .joinGroup(inviteCode)
        .onSuccess { refreshMyGroups() }
        .mapErrorToAppError()

    /**
     * 생성 응답에는 최근 사진·시각이 없어 [MyParfaitGroupVO] 를 세울 수 없다 — 빈 값으로 끼워
     * 넣으면 활동순 정렬이 어긋나므로 목록을 다시 받는다. 그 재조회가 실패해도 생성은 성공이다.
     */
    override suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> = parfaitGroupRemoteDataSource
        .createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = memberLimit,
        ).onSuccess { refreshMyGroups() }
        .mapErrorToAppError()

    /**
     * 응답은 바뀐 닉네임뿐이라 캐시의 멤버 목록에서 "나"를 짚으려면 계정 id 가 필요하다.
     * 그것을 알려면 계정 저장소를 끌어와야 해서, 대신 상세를 서버에서 다시 받는다.
     */
    override suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO> = parfaitGroupRemoteDataSource
        .changeMyNickname(
            groupId = groupId,
            groupNickname = groupNickname,
        ).onSuccess { refreshGroupDetail(groupId) }
        .mapErrorToAppError()

    /** 나간 그룹은 이후 모든 호출이 403 이라 재조회하지 않고 캐시에서 지운다 */
    override suspend fun leaveGroup(groupId: GroupId): Result<GroupId> = parfaitGroupRemoteDataSource
        .leaveGroup(groupId)
        .onSuccess { groupLocalDataSource.removeGroup(groupId) }
        .mapErrorToAppError()

    /** 신고는 서버가 같은 트랜잭션에서 탈퇴로 잇는다 — [leaveGroup] 과 같이 캐시에서 지운다 */
    override suspend fun reportGroup(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO> = parfaitGroupRemoteDataSource
        .reportGroup(
            groupId = groupId,
            reason = reason,
        ).onSuccess { groupLocalDataSource.removeGroup(groupId) }
        .mapErrorToAppError()
}
