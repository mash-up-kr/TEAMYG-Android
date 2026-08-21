package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import kotlinx.coroutines.flow.Flow

interface RecentImageRepository {
    val recentCacheImages: Flow<List<RecentImage>>

    suspend fun addAndGetEvictedCacheFileName(
        uri: String,
        kind: RecentImageKind,
    ): List<String>

    suspend fun removeCacheFileName(values: List<String>)

    suspend fun storeRecentImageInInternalStorage(
        source: String,
        kind: RecentImageKind,
    ): String

    suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean

    suspend fun getLastModifiedCacheFile(sourceUri: String): Long?
}
