package com.teamyg.parfait.feature.login.impl

import com.kakao.sdk.user.UserApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SingletonInjectModule {
    @Provides
    @Singleton
    fun provideUserApiClient(): UserApiClient = UserApiClient.instance
}
