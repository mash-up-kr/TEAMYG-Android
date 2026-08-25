package com.teamyg.parfait.data.repository.canvas

import com.teamyg.parfait.data.source.canvas.local.MyGroupMemberIdLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.repository.canvas.MyGroupMemberIdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MyGroupMemberIdRepositoryImpl
@Inject
constructor(
    private val localDataSource: MyGroupMemberIdLocalDataSource,
) : MyGroupMemberIdRepository {
    override fun observe(groupId: GroupId): Flow<GroupMemberId?> = localDataSource.values
        .map { values -> values[groupId] }
        .distinctUntilChanged()

    override suspend fun save(
        groupId: GroupId,
        groupMemberId: GroupMemberId,
    ) = localDataSource.save(groupId, groupMemberId)
}
