package com.teamyg.parfait.data.source.image.local

import kotlinx.coroutines.flow.Flow

interface RecentImageLocalDataSource {
    val values: Flow<List<String>>

    suspend fun addAndGetEvicted(value: String): List<String>

    suspend fun remove(values: List<String>)
}
