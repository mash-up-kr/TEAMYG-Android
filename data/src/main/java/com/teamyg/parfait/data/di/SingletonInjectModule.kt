package com.teamyg.parfait.data.di

import android.content.Context
import com.teamyg.parfait.data.utils.GalleryMediaProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonInjectModule {
    @Provides
    @Singleton
    fun provideGalleryMediaProvider(
        @ApplicationContext
        context: Context,
    ): GalleryMediaProvider = GalleryMediaProvider(context)
}
