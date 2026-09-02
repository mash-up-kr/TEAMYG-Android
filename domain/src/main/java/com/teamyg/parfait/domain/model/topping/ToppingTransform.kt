package com.teamyg.parfait.domain.model.topping

/**
 * 캔버스 위 토핑의 위치·크기·각도.
 *
 * Double 이 넷 연속이라 평면 파라미터로 두면 호출부가 순서를 뒤바꿔도 컴파일이 통과한다.
 * 이 타입을 만드는 이유가 그것이므로 생성은 항상 named argument 로 한다.
 *
 * 서버에 범위 검증이 없다 — 음수 scale, 캔버스 밖 좌표가 그대로 저장된다.
 * 보정은 화면 계층 책임이다(`api/parfait-image.md`).
 */
data class ToppingTransform(
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
)
