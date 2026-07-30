package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.network.safeApiCall
import com.teamyg.parfait.data.service.TempService
import com.teamyg.parfait.data.source.temp.mapper.toTempVO
import com.teamyg.parfait.domain.model.TempVO
import javax.inject.Inject

class TempRemoteDataSourceImpl @Inject constructor(
    private val tempService: TempService,
) : TempRemoteDataSource {
    override suspend fun getTemp(id: String): Result<TempVO> = safeApiCall { tempService.getTemp(id) }
        .map { it.toTempVO() }
}
