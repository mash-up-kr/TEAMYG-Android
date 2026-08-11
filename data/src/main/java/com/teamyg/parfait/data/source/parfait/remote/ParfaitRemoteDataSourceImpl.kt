package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.domain.model.id.GroupId
import javax.inject.Inject

class ParfaitRemoteDataSourceImpl @Inject constructor(
    private val parfaitService: ParfaitService,
    private val apiCaller: ApiCaller,
) : ParfaitRemoteDataSource {
    override suspend fun getYears(groupId: GroupId): Result<List<Int>> = apiCaller
        .safeApiCall(
            block = { parfaitService.getGroupsByGroupIdParfaitsYear(groupId.value) },
            transform = { it.years },
        )
}
