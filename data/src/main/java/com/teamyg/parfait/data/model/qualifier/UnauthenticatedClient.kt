package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * 자격증명을 붙이지 않는 네트워크 표면(`OkHttpClient`·`Retrofit`·`AuthService`).
 *
 * `AuthInterceptor` 를 달지 않아 `Authorization` 헤더를 붙이지 않고, 인증기를 달지 않아
 * 401 을 만나도 재발급을 시도하지 않는다. 또한 **자기 `Dispatcher`** 를 가진다 — 이 표면을
 * 타는 토큰 재발급이 메인 클라이언트를 타면 `authenticate()` 가 점유한 디스패처 슬롯 뒤에서
 * 큐잉돼 앱 전체 네트워크가 멈춘다.
 *
 * 지금 유일한 사용처는 토큰 재발급이지만, 이름은 사용처가 아니라 이 표면의 성질을 가리킨다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedClient
