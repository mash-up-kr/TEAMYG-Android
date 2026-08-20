package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.network.AuthInterceptor
import okhttp3.Authenticator
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadOkHttpClientTest {
    @Test
    fun provideUploadOkHttpClient_hasNoAuthInterceptor() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 자격증명을 붙이는 인터셉터가 없다 — 붙으면 presigned URL 을 S3 가 거절한다
        assertFalse(client.interceptors.any { it is AuthInterceptor })
    }

    @Test
    fun provideUploadOkHttpClient_hasNoAuthenticator() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 401 을 만나도 재발급을 시도하지 않는다
        assertEquals(Authenticator.NONE, client.authenticator)
    }

    @Test
    fun provideUploadOkHttpClient_doesNotLogRequestBody() {
        // Given·When 업로드 전용 클라이언트를 만든다
        val client = NetworkModule.provideUploadOkHttpClient()

        // Then 본문 로깅이 없다 — 원본 해상도 이미지가 매 업로드마다 문자열로 힙에 올라간다
        val logging = client.interceptors.filterIsInstance<HttpLoggingInterceptor>()
        assertTrue(logging.none { it.level == HttpLoggingInterceptor.Level.BODY })
    }
}
