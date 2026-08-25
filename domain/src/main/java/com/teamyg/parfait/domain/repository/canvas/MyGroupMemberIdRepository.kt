package com.teamyg.parfait.domain.repository.canvas

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import kotlinx.coroutines.flow.Flow

/**
 * 그룹 안에서 "나"를 가리키는 [GroupMemberId] 의 로컬 캐시.
 *
 * 서버는 계정 id([com.teamyg.parfait.domain.model.id.MemberId])와 그룹 멤버십 행 id
 * ([GroupMemberId])를 서로 다른 축으로 발급하고, 어느 응답도 "이게 내 groupMemberId다"를
 * 알려주지 않는다 — 그래서 클라이언트가 직접 추론해 기억해 둔다: 내가 토핑을 새로 놓으면
 * 그 확정 응답의 `placedBy.groupMemberId` 가 곧 나 자신이므로 그 시점에 [save] 한다.
 *
 * 이 기기에서 이 그룹에 한 번도 토핑을 놓은 적이 없으면(재설치·다른 기기 포함) 값이 없다 —
 * 그 경우 "내 토핑" 판별이 불가능하다는 뜻이지 오류가 아니다. 서버가 이 값을 직접 내려주는
 * API 가 생기면 이 캐시는 걷어내도 된다.
 */
interface MyGroupMemberIdRepository {
    fun observe(groupId: GroupId): Flow<GroupMemberId?>

    suspend fun save(
        groupId: GroupId,
        groupMemberId: GroupMemberId,
    )
}
