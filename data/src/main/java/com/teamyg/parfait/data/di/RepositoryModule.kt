package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.repository.auth.AuthRepositoryImpl
import com.teamyg.parfait.data.repository.camera.CameraCacheFileRepositoryImpl
import com.teamyg.parfait.data.repository.gallery.GalleryRepositoryImpl
import com.teamyg.parfait.data.repository.group.ParfaitGroupRepositoryImpl
import com.teamyg.parfait.data.repository.image.ImageSegmentationRepositoryImpl
import com.teamyg.parfait.data.repository.image.ImageUploadRepositoryImpl
import com.teamyg.parfait.data.repository.image.RecentImageRepositoryImpl
import com.teamyg.parfait.data.repository.member.MemberRepositoryImpl
import com.teamyg.parfait.data.repository.parfait.ParfaitRepositoryImpl
import com.teamyg.parfait.data.repository.policy.PolicyRepositoryImpl
import com.teamyg.parfait.data.util.SecureRandomNonceGenerator
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.camera.CameraCacheFileRepository
import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import com.teamyg.parfait.domain.repository.policy.PolicyRepository
import com.teamyg.parfait.domain.util.NonceGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindCameraCacheFileRepository(
        cameraCacheFileRepositoryImpl: CameraCacheFileRepositoryImpl,
    ): CameraCacheFileRepository

    @Binds
    @Singleton
    fun bindGalleryRepository(galleryRepositoryImpl: GalleryRepositoryImpl): GalleryRepository

    @Binds
    @Singleton
    fun bindRecentImageRepository(recentImageRepositoryImpl: RecentImageRepositoryImpl): RecentImageRepository

    @Binds
    @Singleton
    fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindPolicyRepository(policyRepositoryImpl: PolicyRepositoryImpl): PolicyRepository

    @Binds
    @Singleton
    fun bindImageSegmentationRepository(
        imageSegmentationRepositoryImpl: ImageSegmentationRepositoryImpl,
    ): ImageSegmentationRepository

    @Binds
    @Singleton
    fun bindNonceGenerator(secureRandomNonceGenerator: SecureRandomNonceGenerator): NonceGenerator

    @Binds
    @Singleton
    fun bindParfaitGroupRepository(parfaitGroupRepositoryImpl: ParfaitGroupRepositoryImpl): ParfaitGroupRepository

    @Binds
    @Singleton
    fun bindMemberRepository(memberRepositoryImpl: MemberRepositoryImpl): MemberRepository

    @Binds
    @Singleton
    fun bindParfaitRepository(parfaitRepositoryImpl: ParfaitRepositoryImpl): ParfaitRepository

    @Binds
    @Singleton
    fun bindImageUploadRepository(imageUploadRepositoryImpl: ImageUploadRepositoryImpl): ImageUploadRepository
}
