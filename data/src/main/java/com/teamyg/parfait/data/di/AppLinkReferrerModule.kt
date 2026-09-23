package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.installer.deeplink.AppLinkReferrerGateway
import com.teamyg.parfait.data.installer.deeplink.PlayInstallReferrerGateway
import com.teamyg.parfait.data.repository.deeplink.AppLinkReferrerRepositoryImpl
import com.teamyg.parfait.data.source.deeplink.local.AppLinkReferrerLocalDataSource
import com.teamyg.parfait.data.source.deeplink.local.AppLinkReferrerLocalDataSourceImpl
import com.teamyg.parfait.domain.repository.deeplink.AppLinkReferrerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppLinkReferrerModule {
    @Binds
    @Singleton
    fun bindAppLinkReferrerGateway(gateway: PlayInstallReferrerGateway): AppLinkReferrerGateway

    @Binds
    @Singleton
    fun bindAppLinkReferrerLocalDataSource(impl: AppLinkReferrerLocalDataSourceImpl): AppLinkReferrerLocalDataSource

    @Binds
    @Singleton
    fun bindAppLinkReferrerRepository(impl: AppLinkReferrerRepositoryImpl): AppLinkReferrerRepository
}
