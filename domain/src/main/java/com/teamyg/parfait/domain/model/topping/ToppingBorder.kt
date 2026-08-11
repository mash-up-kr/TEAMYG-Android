package com.teamyg.parfait.domain.model.topping

/**
 * 토핑 테두리.
 *
 * 서버는 borderType=SOLID 인데 색이나 두께가 없으면 400 INVALID_BORDER 를 던진다.
 * sealed 로 묶어 그 실패를 표현 불가능한 상태로 만든다 — Solid 를 만들려면 둘 다 있어야 한다.
 *
 * color 는 raw String 이고 앱이 형식을 규정하지 않는다. 서버 계약이 타입만 정하고
 * 형식을 말하지 않아 지금 좁힐 근거가 없다. 색을 실제로 만드는 화면 라운드가 정한다.
 */
sealed interface ToppingBorder {
    data object None : ToppingBorder

    data class Solid(val color: String, val width: Double) : ToppingBorder
}
