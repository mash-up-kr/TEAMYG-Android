package com.teamyg.parfait.data.di.source.image

import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ImageLocalDataSourceModule {
    @Binds
    @Singleton
    fun bindRecentImageLocalDataSource(
        recentImageLocalDataSourceImpl: RecentImageLocalDataSourceImpl,
    ): RecentImageLocalDataSource
}
