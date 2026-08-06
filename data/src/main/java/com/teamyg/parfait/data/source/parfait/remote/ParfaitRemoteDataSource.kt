package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.id.GroupId

interface ParfaitRemoteDataSource {
    suspend fun getYears(groupId: GroupId): Result<List<Int>>
}
