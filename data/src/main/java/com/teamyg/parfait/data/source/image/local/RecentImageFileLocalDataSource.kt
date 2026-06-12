package com.teamyg.parfait.data.source.image.local

interface RecentImageFileLocalDataSource {
    suspend fun copy(sourceUri: String): String

    suspend fun delete(cachedUri: String)

    suspend fun lastModified(cachedUri: String): Long
}
