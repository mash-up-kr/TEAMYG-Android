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
    /** [CanvasPoller][com.teamyg.parfait.data.source.parfait.local.CanvasPoller] 가 하루 경계 판정에 주입받는다 */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
