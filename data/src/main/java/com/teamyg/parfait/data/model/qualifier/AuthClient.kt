package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * 토큰 재발급 전용 네트워크 표면(`OkHttpClient`·`Retrofit`·`AuthService`).
 *
 * 인증기와 `AuthInterceptor` 를 달지 않고 **자기 `Dispatcher`** 를 가진다 — 재발급이
 * 메인 클라이언트를 타면 `authenticate()` 가 점유한 디스패처 슬롯 뒤에서 큐잉돼 앱 전체
 * 네트워크가 멈춘다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient
