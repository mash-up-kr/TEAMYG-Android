package com.teamyg.parfait.data.network

/**
 * 이 엔드포인트의 **본문**은 로그에 남기지 않는다.
 *
 * 응답에 그 자체로 자격증명인 값이 실려 오는 자리에 붙인다 — presigned URL 은 서명을 쿼리
 * 스트링에 싣는 방식이라 URL 한 줄이 곧 업로드 권한이고, `redactHeader` 로는 못 가린다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoBodyLog
