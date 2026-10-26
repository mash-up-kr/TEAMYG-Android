package com.teamyg.parfait.domain.model.topping

/** 텍스트 토핑 정렬. 상수 이름이 곧 서버 `textAlign` 값이다(`name` 으로 보낸다) — 이름을 바꾸면 계약이 깨진다 */
enum class TextToppingAlign {
    LEFT,
    CENTER,
    RIGHT,
}
