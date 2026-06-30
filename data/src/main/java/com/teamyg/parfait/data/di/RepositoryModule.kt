package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.repository.KakaoUserRepositoryImpl
import com.teamyg.parfait.data.repository.camera.CameraCacheFileRepositoryImpl
import com.teamyg.parfait.data.repository.gallery.GalleryRepositoryImpl
import com.teamyg.parfait.data.repository.image.ImageSegmentationRepositoryImpl
import com.teamyg.parfait.data.repository.image.RecentImageRepositoryImpl
import com.teamyg.parfait.domain.repository.KakaoUserRepository
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindKakaoUserRepository(kakaoUserRepositoryImpl: KakaoUserRepositoryImpl): KakaoUserRepository

    @Binds
    @Singleton
    fun bindRecentImageRepository(recentImageRepositoryImpl: RecentImageRepositoryImpl): RecentImageRepository

    @Binds
    @Singleton
    fun bindImageSegmentationRepository(
        imageSegmentationRepositoryImpl: ImageSegmentationRepositoryImpl,
    ): ImageSegmentationRepository

    @Binds
    @Singleton
    fun bindGalleryRepository(galleryRepositoryImpl: GalleryRepositoryImpl): GalleryRepository

    @Binds
    @Singleton
    fun bindCameraCacheFileRepository(
        cameraCacheFileRepositoryImpl: CameraCacheFileRepositoryImpl,
    ): CameraCacheFileRepository
}
