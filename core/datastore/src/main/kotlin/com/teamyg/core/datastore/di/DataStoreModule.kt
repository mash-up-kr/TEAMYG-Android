package com.teamyg.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.teamyg.core.datastore.temp.TempPreferencesDataSource
import com.teamyg.core.datastore.temp.TempPreferencesDataSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val TEMP_PREFERENCES_NAME = "temp_preferences"

private val Context.tempPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TEMP_PREFERENCES_NAME,
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideTempPreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.tempPreferencesDataStore

    @Provides
    @Singleton
    fun provideTempPreferencesDataSource(dataStore: DataStore<Preferences>): TempPreferencesDataSource =
        TempPreferencesDataSourceImpl(dataStore)
}
