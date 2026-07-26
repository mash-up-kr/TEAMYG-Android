package com.teamyg.parfait.data.di.source.file

import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSourceImpl
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface FileLocalDataSourceModule {
    @Binds
    @Singleton
    fun bindFileCameraCacheLocalDataSource(
        fileCameraCacheLocalDataSourceImpl: FileCameraCacheLocalDataSourceImpl,
    ): FileCameraCacheLocalDataSource

    @Binds
    @Singleton
    fun bindFileRecentImageLocalDataSource(
        fileRecentImageLocalDataSourceImpl: FileRecentImageLocalDataSourceImpl,
    ): FileRecentImageLocalDataSource
}
