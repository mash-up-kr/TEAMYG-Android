package com.teamyg.parfait.domain.model.image

/**
 * 업로드 상태. 서버가 주는 값이라 늘어날 수 있어 UNKNOWN 폴백을 둔다.
 *
 * 확인 API 의 성공 응답은 현재 항상 COMPLETED 다 — 서버가 PENDING 인 것만 통과시켜
 * COMPLETED 로 전이시키고 이미 COMPLETED 인 것은 409 로 거르기 때문이다.
 * 그건 서버 구현의 성질이지 계약의 보장이 아니다.
 */
enum class ImageStatus {
    PENDING,
    COMPLETED,
    UNKNOWN,
}
