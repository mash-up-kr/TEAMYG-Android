package com.teamyg.di

import com.teamyg.analytics.logger.Logger
import com.teamyg.analytics.logger.TimberLogger
import com.teamyg.model.qualifier.CoreQualifier
import com.teamyg.model.qualifier.DataQualifier
import com.teamyg.model.qualifier.DomainQualifier
import com.teamyg.model.qualifier.FeatureQualifier
import com.teamyg.model.qualifier.ViewModelQualifier
import com.teamyg.model.tag.LoggerTag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {
    @Provides
    @Singleton
    @FeatureQualifier
    fun provideFeatureLogger(): Logger =
        TimberLogger(
            defaultTag = LoggerTag.FEATURE_TAG,
        )

    @Provides
    @Singleton
    @ViewModelQualifier
    fun provideViewModelLogger(): Logger =
        TimberLogger(
            defaultTag = LoggerTag.VIEWMODEL_TAG,
        )

    @Provides
    @Singleton
    @DomainQualifier
    fun provideDomainLogger(): Logger =
        TimberLogger(
            defaultTag = LoggerTag.DOMAIN_TAG,
        )

    @Provides
    @Singleton
    @DataQualifier
    fun provideDataLogger(): Logger =
        TimberLogger(
            defaultTag = LoggerTag.DATA_TAG,
        )

    @Provides
    @Singleton
    @CoreQualifier
    fun provideCoreLogger(): Logger =
        TimberLogger(
            defaultTag = LoggerTag.CORE_TAG,
        )
}
