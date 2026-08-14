package com.teamyg.parfait.domain.util

/**
 * 로그인 1회분 nonce 를 만든다.
 *
 * 카카오 SDK 요청과 서버 로그인 요청에 **같은 값**을 보내야 한다 — 서버가 ID 토큰의
 * `nonce` 클레임과 대조해 재생 공격을 막는다.
 *
 * 인터페이스로 두는 이유는 테스트에서 값을 고정하기 위해서다.
 */
fun interface NonceGenerator {
    fun generate(): String
}
