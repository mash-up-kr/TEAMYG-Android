package com.teamyg.parfait.data.di.repository.image

import com.teamyg.parfait.data.repository.image.ImageSegmentationRepositoryImpl
import com.teamyg.parfait.data.repository.image.RecentImageRepositoryImpl
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ImageRepositoryModule {
    @Binds
    @Singleton
    fun bindRecentImageRepository(recentImageRepositoryImpl: RecentImageRepositoryImpl): RecentImageRepository

    @Binds
    @Singleton
    fun bindImageSegmentationRepository(
        imageSegmentationRepositoryImpl: ImageSegmentationRepositoryImpl,
    ): ImageSegmentationRepository
}
