package com.teamyg.parfait.domain.repository.image

import kotlinx.coroutines.flow.Flow

interface RecentImageRepository {
    val recentCacheImages: Flow<List<String>>

    suspend fun addRecentImage(uri: String)
}
