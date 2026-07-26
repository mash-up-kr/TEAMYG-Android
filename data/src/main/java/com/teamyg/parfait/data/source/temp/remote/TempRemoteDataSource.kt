package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.data.model.dto.TempDto

interface TempRemoteDataSource {
    suspend fun getTemp(id: String): Result<TempDto>
}
