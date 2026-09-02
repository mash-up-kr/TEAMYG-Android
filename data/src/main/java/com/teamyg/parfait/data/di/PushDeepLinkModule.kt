package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.pushdeeplink.PushDeepLinkBus
import com.teamyg.parfait.domain.repository.pushdeeplink.PushDeepLinkPublisher
import com.teamyg.parfait.domain.repository.pushdeeplink.PushDeepLinkSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PushDeepLinkModule {
    @Provides
    @Singleton
    fun providePushDeepLinkSource(pushDeepLinkBus: PushDeepLinkBus): PushDeepLinkSource = pushDeepLinkBus

    @Provides
    @Singleton
    fun providePushDeepLinkPublisher(pushDeepLinkBus: PushDeepLinkBus): PushDeepLinkPublisher = pushDeepLinkBus
}
