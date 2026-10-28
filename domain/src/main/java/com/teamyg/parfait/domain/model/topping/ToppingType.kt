package com.teamyg.parfait.domain.model.topping

/**
 * 토핑 종류. 서버가 주는 값이라 늘어날 수 있어 UNKNOWN 폴백을 둔다.
 */
enum class ToppingType {
    IMAGE,
    TEXT,
    UNKNOWN,
}
