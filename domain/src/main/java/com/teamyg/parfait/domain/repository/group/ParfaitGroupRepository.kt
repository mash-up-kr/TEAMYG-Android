package com.teamyg.parfait.domain.repository.group

import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.GroupNicknameVO
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId

/**
 * `/api/parfait-groups` 계열 호출. 실패는 모두
 * [com.teamyg.parfait.domain.model.error.AppError] 로 온다.
 */
interface ParfaitGroupRepository {
    /** 탈퇴하지 않은 내 그룹을 활동이 최근인 순서로 준다. 없으면 빈 목록 */
    suspend fun getMyGroups(): Result<List<MyParfaitGroupVO>>

    /**
     * 그룹 하나의 내 닉네임·초대코드·멤버 목록.
     *
     * TODO(서버 응답 확장 대기): 그룹명과 정원이 이 응답에 없다. 서버가 groupName·memberLimit 을
     *  실어 주면 반영한다 — 그때 [GetGroupDetailUseCase] 의 [getMyGroups] 우회 호출을 걷어내고,
     *  그룹 설정 화면의 남은 자리도 고정값 대신 `memberLimit - members.size` 로 바꾼다.
     *  지금은 이름을 [getMyGroups] 에서 따로 집어 오고, 정원은 그룹 생성 응답에만 있어 얻을 길이 없다.
     */
    suspend fun getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO>

    /** 참여 전에 초대코드가 가리키는 그룹명을 확인한다. 참여 상태는 바뀌지 않는다 */
    suspend fun previewJoin(inviteCode: InviteCode): Result<GroupName>

    suspend fun joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO>

    /**
     * @param groupNickname 이 그룹 안에서만 쓰는 닉네임. 전역 닉네임과 다르다
     * @param memberLimit 그룹 정원. 생성 이후 바꿀 수 없다
     */
    suspend fun createGroup(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO>

    suspend fun changeMyNickname(
        groupId: GroupId,
        groupNickname: GroupNickname,
    ): Result<GroupNicknameVO>
}
