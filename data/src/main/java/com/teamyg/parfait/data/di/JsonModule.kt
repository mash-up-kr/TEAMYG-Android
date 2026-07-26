package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JsonModule {
    @Provides
    @Singleton
    @LocalJson
    fun provideLocalJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @RemoteJson
    fun provideRemoteJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
}
