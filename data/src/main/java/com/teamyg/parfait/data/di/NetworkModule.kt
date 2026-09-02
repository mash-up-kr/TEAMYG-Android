package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.BuildConfig
import com.teamyg.parfait.data.model.qualifier.DownloadClient
import com.teamyg.parfait.data.model.qualifier.RemoteJson
import com.teamyg.parfait.data.model.qualifier.UnauthenticatedClient
import com.teamyg.parfait.data.model.qualifier.UploadClient
import com.teamyg.parfait.data.network.AuthInterceptor
import com.teamyg.parfait.data.network.SelectiveLoggingInterceptor
import com.teamyg.parfait.data.network.TokenAuthenticator
import com.teamyg.parfait.data.network.TokenProvider
import com.teamyg.parfait.data.network.TokenStoreTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.Interceptor
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
     * 자격증명을 붙이지 않는 클라이언트. 메인 클라이언트와 **아무것도 공유하지 않는다.**
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
    @UnauthenticatedClient
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .dispatcher(Dispatcher())
        .addInterceptor(loggingInterceptor())
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * S3 presigned PUT 전용. **자격증명을 붙이지 않는 것이 이 클라이언트의 존재 이유다** —
     * presigned URL 에 `Authorization` 이 실리면 S3 가 거절해 업로드가 아예 동작하지 않는다.
     * 재발급 표면([provideUnauthenticatedOkHttpClient])을 재사용하지 않는 이유를 포함한 근거는
     * `parfait/specs/2026-08-20-c106-topping-place-api.md` 업로드 전송 절에 있다.
     *
     * ⚠️ `newBuilder()` 로 파생하면 부모의 [Dispatcher] 를 물려받아 격리가 사라진다.
     * 반드시 새 [OkHttpClient.Builder] 로 만든다.
     *
     * 로깅 인터셉터를 달지 않는다 — presigned URL 은 서명을 쿼리 스트링에 싣는 방식이라
     * URL 자체가 자격증명이고, OkHttp 로깅 인터셉터에는 그것을 가릴 수단이 없다.
     * 이 표면만 `callTimeout` 을 두는 것도 의도다 — `writeTimeout` 은 바이트 사이 유휴
     * 상한이라 전송 전체가 느린 것을 잡지 못한다.
     */
    @Provides
    @Singleton
    @UploadClient
    fun provideUploadOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .dispatcher(Dispatcher())
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(UPLOAD_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(UPLOAD_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 서버 이미지 URL을 그대로 받아오는 전용 클라이언트. 메인 클라이언트와 커넥션 풀·
     * `Dispatcher` 를 공유하지 않는다는 것 말고는 [provideOkHttpClient] 와 같은 프로필이다 —
     * 타임아웃을 늘려야 할 만큼 크다는 근거가 아직 없어 다르게 줄 이유가 없다.
     *
     * ⚠️ `newBuilder()` 로 파생하면 부모의 [Dispatcher] 를 물려받아 격리가 사라진다.
     * 반드시 새 [OkHttpClient.Builder] 로 만든다.
     */
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadOkHttpClient(): OkHttpClient = OkHttpClient
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
    @UnauthenticatedClient
    fun provideUnauthenticatedRetrofit(
        @UnauthenticatedClient okHttpClient: OkHttpClient,
        @RemoteJson json: Json,
    ): Retrofit = Retrofit
        .Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /** 두 클라이언트가 같은 로깅·`Authorization` 마스킹 처리를 받는다 */
    private fun loggingInterceptor(): Interceptor = SelectiveLoggingInterceptor(
        full = httpLoggingInterceptor(HttpLoggingInterceptor.Level.BODY),
        // 본문만 뺀다. BASIC 은 헤더까지 통째로 버려 실패 원인을 좁힐 단서가 사라진다
        redacted = httpLoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS),
    )

    private fun httpLoggingInterceptor(debugLevel: HttpLoggingInterceptor.Level): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) debugLevel else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L
    private const val WRITE_TIMEOUT_SECONDS = 15L
    private const val UPLOAD_WRITE_TIMEOUT_SECONDS = 60L
    private const val UPLOAD_CALL_TIMEOUT_SECONDS = 120L
}
