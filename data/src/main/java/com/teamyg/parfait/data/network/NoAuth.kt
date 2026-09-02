package com.teamyg.parfait.data.network

/**
 * 이 엔드포인트는 `Authorization` 헤더를 붙이지 않는다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoAuth
