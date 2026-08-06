package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.service.PolicyService
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
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun providePolicyService(retrofit: Retrofit): PolicyService = retrofit.create(PolicyService::class.java)

    @Provides
    @Singleton
    fun provideParfaitGroupService(retrofit: Retrofit): ParfaitGroupService =
        retrofit.create(ParfaitGroupService::class.java)

    @Provides
    @Singleton
    fun provideParfaitService(retrofit: Retrofit): ParfaitService = retrofit.create(ParfaitService::class.java)
}
