package com.teamyg.parfait.data.source.canvas.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import kotlinx.coroutines.flow.Flow

interface MyGroupMemberIdLocalDataSource {
    val values: Flow<Map<GroupId, GroupMemberId>>

    suspend fun save(
        groupId: GroupId,
        groupMemberId: GroupMemberId,
    )
}
