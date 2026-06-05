package com.teamyg.segmentation.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.navigation.Navigator
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
    fun provideFeatureSegmentationEntryBuilder(): EntryProviderScope<NavKey>.(Navigator) -> Unit =
        {
            featureSegmentationEntryBuilder(navigator = it)
        }
}
