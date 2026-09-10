package com.teamyg.parfait.analytics.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.teamyg.parfait.analytics.AnalyticsLogger
import com.teamyg.parfait.analytics.FirebaseAnalyticsLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {
    @Binds
    @Singleton
    fun bindAnalyticsLogger(firebaseAnalyticsLogger: FirebaseAnalyticsLogger): AnalyticsLogger
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAnalyticsModule {
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}
