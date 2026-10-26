package com.teamyg.parfait.domain.model.topping

/**
 * 서버 값을 그대로 담는다. 화면의 색·타입과의 변환은 feature `TextToppingWireMapper` 가 맡는다
 * (사진 토핑의 [ToppingTransform] 과 같은 구조).
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
