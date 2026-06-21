package com.teamyg.parfait.domain.repository.image

import kotlinx.coroutines.flow.Flow

interface RecentImageRepository {
    val recentCacheImages: Flow<List<String>>

    suspend fun addAndGetEvictedCacheFileName(value: String): List<String>

    suspend fun removeCacheFileName(values: List<String>)

    suspend fun storeRecentImageInInternalStorage(sourceUri: String): String

    suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean

    suspend fun getLastModifiedCacheFile(sourceUri: String): Long?
}
