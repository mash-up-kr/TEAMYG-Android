package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.source.debug.local.DebugModeLocalDataSource
import com.teamyg.parfait.data.source.debug.local.DebugModeLocalDataSourceImpl
import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileCameraCacheLocalDataSourceImpl
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSourceImpl
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSource
import com.teamyg.parfait.data.source.group.local.GroupLocalDataSourceImpl
import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSource
import com.teamyg.parfait.data.source.image.local.ImageFileLocalDataSourceImpl
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSourceImpl
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSource
import com.teamyg.parfait.data.source.member.local.UserInfoLocalDataSourceImpl
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSource
import com.teamyg.parfait.data.source.parfait.local.CanvasLocalDataSourceImpl
import com.teamyg.parfait.data.source.token.local.EncryptedTokenStore
import com.teamyg.parfait.data.source.token.local.TokenStore
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSourceImpl
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
    fun bindImageFileLocalDataSource(
        imageFileLocalDataSourceImpl: ImageFileLocalDataSourceImpl,
    ): ImageFileLocalDataSource

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

    @Binds
    @Singleton
    fun bindGroupLocalDataSource(groupLocalDataSourceImpl: GroupLocalDataSourceImpl): GroupLocalDataSource

    @Binds
    @Singleton
    fun bindCanvasLocalDataSource(canvasLocalDataSourceImpl: CanvasLocalDataSourceImpl): CanvasLocalDataSource

    @Binds
    @Singleton
    fun bindToppingDraftLocalDataSource(
        toppingDraftLocalDataSourceImpl: ToppingDraftLocalDataSourceImpl,
    ): ToppingDraftLocalDataSource

    @Binds
    @Singleton
    fun bindDebugModeLocalDataSource(
        debugModeLocalDataSourceImpl: DebugModeLocalDataSourceImpl,
    ): DebugModeLocalDataSource
}
