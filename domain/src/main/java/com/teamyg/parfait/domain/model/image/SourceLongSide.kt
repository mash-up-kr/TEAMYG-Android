package com.teamyg.parfait.domain.model.image

/**
 * 누끼를 오려낸 사진 전체의 긴 변(픽셀). 잘린 알맹이의 긴 변이 아니다.
 *
 * 무엇을 「원본」으로 보는지, 벌거벗은 `Int` 를 쓰지 않는 이유는
 * `specs/2026-09-09-topping-upload-source-scaled.md`.
 */
@JvmInline
value class SourceLongSide(val px: Int)
