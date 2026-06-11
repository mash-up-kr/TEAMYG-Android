package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.source.image.local.RecentImageFileLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentImageRepositoryImpl
@Inject
constructor(
    private val recentImageLocalDataSource: RecentImageLocalDataSource,
    private val recentImageFileLocalDataSource: RecentImageFileLocalDataSource,
) : RecentImageRepository {
    override val recentCacheImages: Flow<List<String>>
        get() = recentImageLocalDataSource.values
            .distinctUntilChanged()

    override suspend fun addRecentImage(uri: String) {
        val stableUri: String = runCatching { recentImageFileLocalDataSource.copy(uri) }
            .getOrNull()
            ?: return

        val evicted: List<String> = recentImageLocalDataSource.addAndGetEvicted(stableUri)

        evicted.forEach {
            recentImageFileLocalDataSource.delete(it)
        }
    }
}
