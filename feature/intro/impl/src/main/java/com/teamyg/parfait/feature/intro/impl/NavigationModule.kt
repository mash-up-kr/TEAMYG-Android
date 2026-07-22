package com.teamyg.parfait.feature.intro.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {
    @IntoSet
    @Provides
    fun provideFeatureSplashEntryBuilder(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        featureSplashEntryBuilder(navigator = it)
    }

    @IntoSet
    @Provides
    fun provideFeatureTermAgreeEntryBuilder(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        featureTermAgreeEntryBuilder(navigator = it)
    }
}
