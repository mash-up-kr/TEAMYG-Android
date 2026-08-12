package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSource
import com.teamyg.parfait.data.source.auth.remote.AuthRemoteDataSourceImpl
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSource
import com.teamyg.parfait.data.source.group.remote.ParfaitGroupRemoteDataSourceImpl
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSource
import com.teamyg.parfait.data.source.image.remote.ImageRemoteDataSourceImpl
import com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSource
import com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSourceImpl
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSource
import com.teamyg.parfait.data.source.parfait.remote.ParfaitRemoteDataSourceImpl
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSourceImpl
import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSource
import com.teamyg.parfait.data.source.policy.remote.PolicyRemoteDataSourceImpl
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
    fun bindAuthRemoteDataSource(authRemoteDataSourceImpl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    @Singleton
    fun bindPolicyRemoteDataSource(policyRemoteDataSourceImpl: PolicyRemoteDataSourceImpl): PolicyRemoteDataSource

    @Binds
    @Singleton
    fun bindParfaitGroupRemoteDataSource(
        parfaitGroupRemoteDataSourceImpl: ParfaitGroupRemoteDataSourceImpl,
    ): ParfaitGroupRemoteDataSource

    @Binds
    @Singleton
    fun bindParfaitRemoteDataSource(parfaitRemoteDataSourceImpl: ParfaitRemoteDataSourceImpl): ParfaitRemoteDataSource

    @Binds
    @Singleton
    fun bindImageRemoteDataSource(imageRemoteDataSourceImpl: ImageRemoteDataSourceImpl): ImageRemoteDataSource

    @Binds
    @Singleton
    fun bindMemberRemoteDataSource(memberRemoteDataSourceImpl: MemberRemoteDataSourceImpl): MemberRemoteDataSource

    @Binds
    @Singleton
    fun bindParfaitImageRemoteDataSource(
        parfaitImageRemoteDataSourceImpl: ParfaitImageRemoteDataSourceImpl,
    ): ParfaitImageRemoteDataSource
}
