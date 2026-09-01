package com.teamyg.parfait.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    /** 테스트가 시각을 고정할 수 있도록, 시각을 보는 곳이 [Clock] 을 주입으로 받게 한다 */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
