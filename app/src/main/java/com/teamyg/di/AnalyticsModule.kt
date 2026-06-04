package com.teamyg.di

import com.teamyg.analytics.AnalyticsHelper
import com.teamyg.analytics.YGAnalyticsHelper
import com.teamyg.analytics.logger.Logger
import com.teamyg.model.qualifier.CoreQualifier
import com.teamyg.model.qualifier.DataQualifier
import com.teamyg.model.qualifier.DomainQualifier
import com.teamyg.model.qualifier.FeatureQualifier
import com.teamyg.model.qualifier.ViewModelQualifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    @FeatureQualifier
    fun provideFeatureAnalyticsHelper(
        @FeatureQualifier logger: Logger,
    ): AnalyticsHelper =
        YGAnalyticsHelper(
            logger = logger,
        )

    @Provides
    @Singleton
    @ViewModelQualifier
    fun provideViewModelAnalyticsHelper(
        @ViewModelQualifier logger: Logger,
    ): AnalyticsHelper =
        YGAnalyticsHelper(
            logger = logger,
        )

    @Provides
    @Singleton
    @DomainQualifier
    fun provideDomainAnalyticsHelper(
        @DomainQualifier logger: Logger,
    ): AnalyticsHelper =
        YGAnalyticsHelper(
            logger = logger,
        )

    @Provides
    @Singleton
    @DataQualifier
    fun provideDataAnalyticsHelper(
        @DataQualifier logger: Logger,
    ): AnalyticsHelper =
        YGAnalyticsHelper(
            logger = logger,
        )

    @Provides
    @Singleton
    @CoreQualifier
    fun provideCoreAnalyticsHelper(
        @CoreQualifier logger: Logger,
    ): AnalyticsHelper =
        YGAnalyticsHelper(
            logger = logger,
        )
}
