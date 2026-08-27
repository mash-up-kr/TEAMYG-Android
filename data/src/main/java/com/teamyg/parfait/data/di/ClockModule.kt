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
    /** 전역 [Clock] 싱글턴 바인딩. 시각을 보는 곳이면 어디든 테스트에서 고정할 수 있게 주입받는다 */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
