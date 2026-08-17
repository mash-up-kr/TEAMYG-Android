package com.teamyg.parfait.data.source.group.local

import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupLocalDataSourceImpl @Inject constructor() : GroupLocalDataSource {
    private val _myGroups = MutableStateFlow<List<MyParfaitGroupVO>?>(null)
    override val myGroups: StateFlow<List<MyParfaitGroupVO>?> = _myGroups.asStateFlow()

    private val details = MutableStateFlow<Map<GroupId, ParfaitGroupDetailVO>>(emptyMap())

    /**
     * 맵 전체가 아니라 한 그룹으로 좁혀서 낸다 — [distinctUntilChanged] 가 없으면 남의 그룹
     * 상세가 저장될 때마다 이 구독자까지 재방출된다.
     */
    override fun groupDetail(groupId: GroupId): Flow<ParfaitGroupDetailVO?> = details
        .map { it[groupId] }
        .distinctUntilChanged()

    override fun saveMyGroups(groups: List<MyParfaitGroupVO>) {
        _myGroups.value = groups
    }

    override fun saveGroupDetail(detail: ParfaitGroupDetailVO) {
        details.update { it + (detail.groupId to detail) }
    }

    override fun removeGroup(groupId: GroupId) {
        _myGroups.update { current -> current?.filterNot { it.groupId == groupId } }
        details.update { it - groupId }
    }

    override fun clear() {
        _myGroups.value = null
        details.value = emptyMap()
    }
}
