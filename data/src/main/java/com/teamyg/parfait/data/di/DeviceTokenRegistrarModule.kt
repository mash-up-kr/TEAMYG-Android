package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.notification.DeviceTokenRegistrarImpl
import com.teamyg.parfait.domain.notification.DeviceTokenRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DeviceTokenRegistrarModule {
    @Binds
    @Singleton
    fun bindDeviceTokenRegistrar(impl: DeviceTokenRegistrarImpl): DeviceTokenRegistrar
}
