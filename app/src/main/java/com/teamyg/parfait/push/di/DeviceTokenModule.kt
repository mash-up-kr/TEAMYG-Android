package com.teamyg.parfait.push.di

import com.teamyg.parfait.domain.notification.DeviceTokenProvider
import com.teamyg.parfait.push.FirebaseDeviceTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DeviceTokenModule {
    @Binds
    @Singleton
    fun bindDeviceTokenProvider(firebaseDeviceTokenProvider: FirebaseDeviceTokenProvider): DeviceTokenProvider
}
