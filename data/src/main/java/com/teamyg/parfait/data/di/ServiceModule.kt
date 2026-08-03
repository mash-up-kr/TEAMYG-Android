package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.service.TempService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideTempService(retrofit: Retrofit): TempService = retrofit.create(TempService::class.java)
}
