package com.teamyg.parfait.data.di.network

import com.teamyg.parfait.data.BuildConfig
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.network.AuthInterceptor
import com.teamyg.parfait.data.network.EmptyTokenProvider
import com.teamyg.parfait.data.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = EmptyTokenProvider()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            },
        ).connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @RemoteJson json: Json,
    ): Retrofit = Retrofit
        .Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private const val TIMEOUT_SECONDS = 30L
}
