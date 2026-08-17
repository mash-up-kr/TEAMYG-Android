package com.teamyg.parfait.domain.repository.group

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
import kotlinx.coroutines.flow.Flow

/**
 * `/api/parfait-groups` 계열 호출. 실패는 모두
 * [com.teamyg.parfait.domain.model.error.AppError] 로 온다.
 */
interface ParfaitGroupRepository {
    /**
     * 캐시된 내 그룹 목록. `null` 은 아직 한 번도 받지 못했다는 뜻이고 빈 목록과 다르다.
     * 값을 새로 받으려면 [refreshMyGroups] 를 부른다 — 이 흐름은 스스로 조회하지 않는다.
     */
    val myGroups: Flow<List<MyParfaitGroupVO>?>

    /** 캐시된 그룹 상세. 받아 둔 것이 없으면 `null` */
    fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>

    /**
     * 서버에서 목록을 다시 받아 캐시를 덮는다. 실패하면 캐시는 그대로다.
     *
     * 값을 돌려주지 않는 이유: 읽는 길이 [myGroups] 하나여야 한다. 반환값으로도 줄 수 있으면
     * 화면이 그것을 쓰기 시작하고 캐시는 두 번째 출처가 된다.
     */
    suspend fun refreshMyGroups(): Result<Unit>

    /**
     * 서버에서 그 그룹 상세를 다시 받아 캐시를 덮는다. 실패하면 캐시는 그대로다.
     *
     * TODO(서버 응답 확장 대기): 그룹명과 정원이 이 응답에 없다. 서버가 groupName·memberLimit 을
     *  실어 주면 반영한다 — 그때 [GetGroupDetailUseCase] 의 [refreshMyGroups] 우회 호출을 걷어내고,
     *  그룹 설정 화면의 남은 자리도 고정값 대신 `memberLimit - members.size` 로 바꾼다.
     *  지금은 이름을 [refreshMyGroups] 에서 따로 집어 오고, 정원은 그룹 생성 응답에만 있어 얻을 길이 없다.
     */
    suspend fun refreshGroupDetail(groupId: GroupId): Result<Unit>

    /** 세션이 끝났을 때 캐시를 비운다. 인메모리라 suspend 가 아니다 */
    fun clearGroups()

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

    /**
     * 내 멤버십만 지운다. 그룹도, 내가 올린 사진도 남는다.
     *
     * 나간 뒤에는 그 그룹의 상세·닉네임 변경·신고가 모두 403 GROUP_NOT_JOINED 로 떨어진다 —
     * 성공했다면 그 그룹을 가리키는 화면은 더 이상 열어 둘 수 없다.
     */
    suspend fun leaveGroup(groupId: GroupId): Result<GroupId>

    /**
     * @param reason 신고 사유. 서버 규칙을 벗어나면 400 INVALID_GROUP_REPORT_REASON 이다
     */
    suspend fun reportGroup(
        groupId: GroupId,
        reason: String,
    ): Result<ReportedGroupVO>
}
