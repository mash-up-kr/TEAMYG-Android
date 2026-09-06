package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.event.SessionEventBusImpl
import com.teamyg.parfait.domain.event.SessionEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {
    @Provides
    @Singleton
    fun provideSessionEventBus(impl: SessionEventBusImpl): SessionEventBus = impl
}
