package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.utils.image.UploadImagePreprocessor
import com.teamyg.parfait.data.utils.image.UploadImagePreprocessorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UtilsModule {
    @Binds
    @Singleton
    fun bindUploadImagePreprocessor(uploadImagePreprocessorImpl: UploadImagePreprocessorImpl): UploadImagePreprocessor
}
