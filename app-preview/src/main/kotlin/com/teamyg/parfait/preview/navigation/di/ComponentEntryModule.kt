package com.teamyg.parfait.preview.navigation.di

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.navigation.entry.componentEntryBuilders
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
object ComponentEntryModule {
    @IntoSet
    @Provides
    fun provideComponentEntryBuilders(): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        componentEntryBuilders(navigator = it)
    }
}
