package com.teamyg.parfait.feature.common.terms.impl.navigation

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
    fun provideFeatureCommonTermsEntryBuilder(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        featureCommonTermsEntryBuilder(navigator = it)
    }
}
