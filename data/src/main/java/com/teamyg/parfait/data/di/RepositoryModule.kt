package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.repository.KakaoUserRepositoryImpl
import com.teamyg.parfait.domain.repository.KakaoUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindKakaoUserRepository(kakaoUserRepositoryImpl: KakaoUserRepositoryImpl): KakaoUserRepository
}
