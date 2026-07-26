package com.teamyg.parfait.data.di.repository.gallery

import com.teamyg.parfait.data.repository.gallery.GalleryRepositoryImpl
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface GalleryRepositoryModule {
    @Binds
    @Singleton
    fun bindGalleryRepository(galleryRepositoryImpl: GalleryRepositoryImpl): GalleryRepository
}
