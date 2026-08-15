package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSourceImpl
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSourceImpl
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSourceImpl
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSourceImpl
import com.teamyg.parfait.data.source.token.local.EncryptedTokenStore
import com.teamyg.parfait.data.source.token.local.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface LocalDataSourceModule {
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

    @Binds
    @Singleton
    fun bindRecentImageLocalDataSource(
        recentImageLocalDataSourceImpl: RecentImageLocalDataSourceImpl,
    ): RecentImageLocalDataSource

    @Binds
    @Singleton
    fun bindTokenStore(encryptedTokenStore: EncryptedTokenStore): TokenStore

    @Binds
    @Singleton
    fun bindUserInfoLocalDataSource(userInfoLocalDataSourceImpl: UserInfoLocalDataSourceImpl): UserInfoLocalDataSource
}
