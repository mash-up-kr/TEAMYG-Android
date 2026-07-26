package com.teamyg.parfait.data.di.repository.camera

import com.teamyg.parfait.data.repository.camera.CameraCacheFileRepositoryImpl
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CameraRepositoryModule {
    @Binds
    @Singleton
    fun bindCameraCacheFileRepository(
        cameraCacheFileRepositoryImpl: CameraCacheFileRepositoryImpl,
    ): CameraCacheFileRepository
}
