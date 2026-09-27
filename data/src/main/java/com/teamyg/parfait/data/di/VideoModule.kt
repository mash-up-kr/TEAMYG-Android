package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.provider.CanvasVideoEncoderImpl
import com.teamyg.parfait.domain.provider.CanvasVideoEncoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface VideoModule {
    @Binds
    @Singleton
    fun bindCanvasVideoEncoder(canvasVideoEncoderImpl: CanvasVideoEncoderImpl): CanvasVideoEncoder
}
