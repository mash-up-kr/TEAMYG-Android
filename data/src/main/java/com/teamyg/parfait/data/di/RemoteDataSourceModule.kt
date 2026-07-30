package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.source.temp.remote.TempRemoteDataSource
import com.teamyg.parfait.data.source.temp.remote.TempRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RemoteDataSourceModule {
    @Binds
    @Singleton
    fun bindTempRemoteDataSource(tempRemoteDataSourceImpl: TempRemoteDataSourceImpl): TempRemoteDataSource
}
