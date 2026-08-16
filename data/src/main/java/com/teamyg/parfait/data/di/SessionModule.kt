package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.session.SessionEventBus
import com.teamyg.parfait.domain.repository.session.SessionEventSource
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
    fun provideSessionEventSource(sessionEventBus: SessionEventBus): SessionEventSource = sessionEventBus
}
