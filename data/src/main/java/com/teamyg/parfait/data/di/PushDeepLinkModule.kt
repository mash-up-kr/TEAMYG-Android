package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.event.PushDeepLinkEventBusImpl
import com.teamyg.parfait.domain.event.PushDeepLinkEventBus
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
