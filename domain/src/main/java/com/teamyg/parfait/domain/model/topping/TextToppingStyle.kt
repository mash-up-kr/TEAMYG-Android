package com.teamyg.parfait.domain.model.topping

/**
 * 텍스트 토핑의 글자 모양.
 *
 * @param color 서버가 형식을 검사하지 않는다 — 다른 클라이언트가 넣은 값은 파싱이 실패할 수 있다
 *   (`api/parfait-text.md`).
 * @param fontSize 확대 전 기준 크기다. 화면 크기는 [ToppingTransform.scale] 을 곱한 값이다.
 */
data class TextToppingStyle(
    val color: String,
    val fontSize: Double,
    val isFill: Boolean,
    val isStroke: Boolean,
)
