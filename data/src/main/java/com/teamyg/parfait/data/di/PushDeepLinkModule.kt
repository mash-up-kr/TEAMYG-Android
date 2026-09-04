package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.push.PushDeepLinkEventBusImpl
import com.teamyg.parfait.domain.repository.push.PushDeepLinkEventBus
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
    fun providePushDeepLinkEventBus(impl: PushDeepLinkEventBusImpl): PushDeepLinkEventBus = impl
}
