package com.teamyg.parfait.feature.login.impl

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
    fun provideFeatureLoginEntryBuilder(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        featureLoginEntryBuilder(navigator = it)
    }
}
