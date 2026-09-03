package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.push.PushDeepLinkBus
import com.teamyg.parfait.domain.repository.push.PushDeepLinkPublisher
import com.teamyg.parfait.domain.repository.push.PushDeepLinkSource
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
