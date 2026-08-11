package com.teamyg.parfait.domain.model.member

/**
 * 서버가 주는 값이라 UNKNOWN 폴백을 둔다. enumValueOf 로 바꾸면 서버가 provider 를
 * 하나 늘리는 순간 크래시한다. 실제로 서버 영속 계층에는 GOOGLE 이 있는데 core enum 에는
 * 없는 상태다(`api/member.md`).
 */
enum class LoginProvider {
    KAKAO,
    APPLE,
    UNKNOWN,
}
