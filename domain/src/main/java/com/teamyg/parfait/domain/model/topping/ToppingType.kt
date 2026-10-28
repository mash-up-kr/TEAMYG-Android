package com.teamyg.parfait.domain.model.topping

/**
 * 토핑 종류(이미지·텍스트). 서버가 주는 값이라 늘어날 수 있어 UNKNOWN 폴백을 둔다.
 * 텍스트 토핑의 글자 스타일은 이것이 아니라 canvas 의 `TextToppingType` 이다.
 */
enum class ToppingType {
    IMAGE,
    TEXT,
    UNKNOWN,
}
