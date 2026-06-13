package com.teamyg.parfait.data.source.image.local

interface RecentImageFileLocalDataSource {
    suspend fun store(sourceUri: String): String

    suspend fun delete(cachedUri: String)

    suspend fun getLastModified(cachedUri: String): Long
}
