package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.model.qualifier.AuthClient
import com.teamyg.parfait.data.service.AuthService
import com.teamyg.parfait.data.service.ImageService
import com.teamyg.parfait.data.service.MemberService
import com.teamyg.parfait.data.service.ParfaitGroupService
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.service.ParfaitService
import com.teamyg.parfait.data.service.PolicyService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)

    /**
     * 재발급 전용 [AuthService]. 인증기가 붙지 않은 [AuthClient] 클라이언트를 타므로
     * [com.teamyg.parfait.data.network.TokenAuthenticator] 가 이것을 써도 Dagger 순환이
     * 생기지 않고, 재발급이 메인 `Dispatcher` 슬롯을 두고 경합하지도 않는다.
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthClientAuthService(@AuthClient retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun providePolicyService(retrofit: Retrofit): PolicyService = retrofit.create(PolicyService::class.java)

    @Provides
    @Singleton
    fun provideParfaitGroupService(retrofit: Retrofit): ParfaitGroupService =
        retrofit.create(ParfaitGroupService::class.java)

    @Provides
    @Singleton
    fun provideParfaitService(retrofit: Retrofit): ParfaitService = retrofit.create(ParfaitService::class.java)

    @Provides
    @Singleton
    fun provideImageService(retrofit: Retrofit): ImageService = retrofit.create(ImageService::class.java)

    @Provides
    @Singleton
    fun provideMemberService(retrofit: Retrofit): MemberService = retrofit.create(MemberService::class.java)

    @Provides
    @Singleton
    fun provideParfaitImageService(retrofit: Retrofit): ParfaitImageService =
        retrofit.create(ParfaitImageService::class.java)
}
