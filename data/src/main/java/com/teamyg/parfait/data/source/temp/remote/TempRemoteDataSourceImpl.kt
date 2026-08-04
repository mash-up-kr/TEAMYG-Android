package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.TempService
import com.teamyg.parfait.data.source.temp.mapper.toTempVO
import com.teamyg.parfait.domain.model.TempVO
import javax.inject.Inject

class TempRemoteDataSourceImpl @Inject constructor(
    private val tempService: TempService,
    private val apiCaller: ApiCaller,
) : TempRemoteDataSource {
    override suspend fun getTemp(id: String): Result<TempVO> = apiCaller
        .safeApiCall { tempService.getTemp(id) }
        .map { it.toTempVO() }
}
