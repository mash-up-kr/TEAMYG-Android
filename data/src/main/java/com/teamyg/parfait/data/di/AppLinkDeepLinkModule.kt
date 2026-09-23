package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.event.AppLinkDeepLinkEventBusImpl
import com.teamyg.parfait.domain.event.AppLinkDeepLinkEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppLinkDeepLinkModule {
    @Provides
    @Singleton
    fun provideAppLinkDeepLinkEventBus(impl: AppLinkDeepLinkEventBusImpl): AppLinkDeepLinkEventBus = impl
}
