package com.teamyg.parfait.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private const val PARFAIT_PREFERENCES_NAME = "parfait_preferences"

private val Context.parfaitPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PARFAIT_PREFERENCES_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideParfaitPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.parfaitPreferencesDataStore

    @Provides
    @Singleton
    fun provideDataStoreJsonParser(): Json = Json {
        ignoreUnknownKeys = true
    }
}
