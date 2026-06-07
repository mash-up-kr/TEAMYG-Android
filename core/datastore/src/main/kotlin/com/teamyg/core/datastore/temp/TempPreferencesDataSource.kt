package com.teamyg.core.datastore.temp

import kotlinx.coroutines.flow.Flow

interface TempPreferencesDataSource {
    val accessToken: Flow<String?>

    suspend fun setAccessToken(token: String)

    suspend fun clearAccessToken()
}
