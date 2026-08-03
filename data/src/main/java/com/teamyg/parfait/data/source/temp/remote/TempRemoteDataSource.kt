package com.teamyg.parfait.data.source.temp.remote

import com.teamyg.parfait.domain.model.TempVO

interface TempRemoteDataSource {
    suspend fun getTemp(id: String): Result<TempVO>
}
