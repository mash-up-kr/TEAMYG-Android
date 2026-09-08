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

    companion object {
        /**
         * 굵기가 들 수 있는 화면 dp 범위. 서버가 범위를 검증하지 않으므로(`api/parfait-image.md`)
         * 앱이 가두고, 플랫폼마다 상한이 다르면 같은 캔버스가 기기마다 다르게 보인다.
         */
        val WIDTH_RANGE_DP = 2.0..30.0

        /** 서버에 이미 범위 밖으로 저장된 행이 있어, 슬라이더만 좁히면 그 행이 계속 굵게 그려진다 */
        fun solidClamped(
            color: String,
            width: Double,
        ): Solid = Solid(color = color, width = width.coerceIn(WIDTH_RANGE_DP))
    }
}
