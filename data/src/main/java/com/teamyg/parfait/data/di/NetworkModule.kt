package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.BuildConfig
import com.teamyg.parfait.data.model.qualifier.AuthClient
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.network.AuthInterceptor
import com.teamyg.parfait.data.network.TokenAuthenticator
import com.teamyg.parfait.data.network.TokenProvider
import com.teamyg.parfait.data.network.TokenStoreTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
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
    fun provideTokenProvider(tokenStoreTokenProvider: TokenStoreTokenProvider): TokenProvider = tokenStoreTokenProvider

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthInterceptor = AuthInterceptor(tokenProvider)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor())
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 토큰 재발급 전용 클라이언트. 메인 클라이언트와 **아무것도 공유하지 않는다.**
     *
     * `authenticate()` 는 그 호출이 아직 `Dispatcher` 슬롯을 점유한 채 `runBlocking` 으로
     * 블록된 상태에서 실행된다. 재발급이 같은 클라이언트를 타면 같은 `Dispatcher` ·같은
     * 호스트를 쓰므로, 같은 호스트 요청이 기본 한도(`maxRequestsPerHost = 5`)만큼 동시에
     * 401 을 맞는 순간 재발급 요청은 `readyAsyncCalls` 에서 영원히 승격되지 못한다.
     * `callTimeout` 도 없어 스스로 풀리지 않는다 — 앱의 모든 네트워크가 멈춘다.
     *
     * 그래서 [Dispatcher] 를 직접 새로 만든다. `okHttpClient.newBuilder()` 로 파생하면
     * **부모의 `Dispatcher` 인스턴스를 그대로 물려받아** 교착이 살아남는다.
     *
     * 인증기를 달지 않는 이유는 재발급 자신의 401 이 인증기를 재진입시키지 않게 하기
     * 위해서고, `AuthInterceptor` 를 달지 않는 이유는 재발급이 자격증명을 헤더가 아니라
     * **본문**(`ReissueRequest.refreshToken`)으로 보내기 때문이다.
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .dispatcher(Dispatcher())
        .addInterceptor(loggingInterceptor())
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

    @Provides
    @Singleton
    @AuthClient
    fun provideAuthRetrofit(
        @AuthClient okHttpClient: OkHttpClient,
        @RemoteJson json: Json,
    ): Retrofit = Retrofit
        .Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** 두 클라이언트가 같은 로깅·`Authorization` 마스킹 처리를 받는다 */
    private fun loggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L
    private const val WRITE_TIMEOUT_SECONDS = 15L
}
