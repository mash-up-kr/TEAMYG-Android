package com.teamyg.parfait.data.di

import com.teamyg.parfait.data.network.AuthInterceptor
import com.teamyg.parfait.data.network.TokenAuthenticator
import com.teamyg.parfait.data.network.TokenProvider
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * 재발급 전용 클라이언트가 메인 클라이언트와 **무엇을 공유하지 않는지**를 못박는다.
 *
 * 이건 동작이 아니라 배선의 구조적 성질이라 [dagger.Provides] 함수를 직접 불러 확인한다.
 * 실제 데드락(같은 `Dispatcher` 의 per-host 슬롯이 전부 블록된 인증기에 잡혀 재발급이
 * 승격되지 못하는 상황)을 재현하는 테스트는 만들지 않는다 — 회귀했을 때 "실패"가 아니라
 * "무한 대기"로 나타나 CI 가 타임아웃 없이 걸린다. 여기서 고정하는 건 원인 쪽이다.
 */
class NetworkModuleTest {
    private val tokenAuthenticator: TokenAuthenticator = mockk()
    private val authInterceptor = AuthInterceptor(object : TokenProvider {
        override fun getToken(): String? = null
    })

    private val mainClient = NetworkModule.provideOkHttpClient(
        authInterceptor = authInterceptor,
        tokenAuthenticator = tokenAuthenticator,
    )
    private val authClient = NetworkModule.provideAuthOkHttpClient()

    @Test
    fun authOkHttpClient_hasItsOwnDispatcher() {
        // Given 메인 클라이언트와 재발급 전용 클라이언트
        // When 두 클라이언트의 디스패처를 본다
        // Then 서로 다른 인스턴스다 — 같으면 인증기가 점유한 슬롯 뒤에 재발급이 줄 서서
        // per-host 한도(기본 5)가 차는 순간 앱 전체 네트워크가 멈춘다.
        // `newBuilder()` 로 파생하면 부모 디스패처를 그대로 물려받아 이 단언이 깨진다
        assertNotSame(mainClient.dispatcher, authClient.dispatcher)
    }

    @Test
    fun authOkHttpClient_doesNotCarryAuthenticator() {
        // Given 메인 클라이언트에는 인증기가 달려 있다
        assertSame(tokenAuthenticator, mainClient.authenticator)

        // When 재발급 전용 클라이언트의 인증기를 본다
        // Then 달려 있지 않다 — 재발급 자신의 401 이 인증기를 재진입시키면 안 된다
        assertNotSame(tokenAuthenticator, authClient.authenticator)
    }

    @Test
    fun authOkHttpClient_doesNotCarryAuthInterceptor() {
        // Given 메인 클라이언트에는 AuthInterceptor 가 달려 있다
        assertSame(authInterceptor, mainClient.interceptors.first { it is AuthInterceptor })

        // When 재발급 전용 클라이언트의 인터셉터 목록을 본다
        // Then AuthInterceptor 가 없다 — 재발급은 자격증명을 헤더가 아니라 본문으로 보낸다
        assertFalse(authClient.interceptors.any { it is AuthInterceptor })
    }
}
