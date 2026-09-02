package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.installer.image.ModuleInstallGateway
import com.teamyg.parfait.data.installer.image.PlayServicesModuleInstallGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface ModuleInstallModule {
    @Binds
    @Singleton
    fun bindModuleInstallGateway(gateway: PlayServicesModuleInstallGateway): ModuleInstallGateway
}
