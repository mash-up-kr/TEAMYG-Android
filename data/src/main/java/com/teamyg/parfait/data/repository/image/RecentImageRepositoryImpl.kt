package com.teamyg.parfait.data.repository.image

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
) : RecentImageRepository {
    override val recentCacheImages: Flow<List<String>>
        get() = recentImageLocalDataSource.values
            .distinctUntilChanged()

    override suspend fun addRecentImage(uri: String) {
        recentImageLocalDataSource.add(uri)
    }
}
