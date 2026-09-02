package com.teamyg.parfait.data.source.group.local

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 그룹 목록·상세의 인메모리 SSoT. 프로세스와 수명을 같이 하고 디스크에 남기지 않는다
 * (ADR-0023) — 그래서 모든 함수가 suspend 가 아니다.
 */
interface GroupLocalDataSource {
    /** `null` 은 **아직 한 번도 받지 못했다**는 뜻이다. 빈 목록(그룹 0건)과 구분한다 */
    val myGroups: StateFlow<List<MyParfaitGroupVO>?>

    /** 그 그룹의 상세. 캐시에 없으면 `null` 을 낸다 */
    fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?>

    fun saveMyGroups(groups: List<MyParfaitGroupVO>)

    fun saveGroupDetail(detail: ParfaitGroupDetailVO)

    /** 나간 그룹을 목록과 상세 **양쪽에서** 지운다 */
    fun removeGroup(groupId: GroupId)

    /** 세션이 끝났을 때. 미조회 상태로 되돌린다 */
    fun clear()
}
