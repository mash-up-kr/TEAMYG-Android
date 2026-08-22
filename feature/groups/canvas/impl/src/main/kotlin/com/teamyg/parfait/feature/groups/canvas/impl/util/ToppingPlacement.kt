package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.teamyg.parfait.domain.model.topping.ToppingTransform

/**
 * 배치 화면의 화면 좌표를 서버가 저장하는 정규화 좌표로 바꾼다.
 *
 * ⚠️ **쓰기 쪽 `scale` 과 읽기 쪽 `scale` 은 기준이 다른 다른 수다.** 그대로 보내면 캔버스에서
 * 크기가 달라진다. 계산식 근거는 `specs/2026-08-20-c106-topping-place-api.md`의 「좌표 변환」 절 참고.
 *
 * 위치는 좌상단이 아니라 **중심** 기준이다.
 */
internal fun toToppingTransform(
    offsetX: Dp,
    offsetY: Dp,
    scale: Float,
    rotationDegrees: Float,
    canvasSize: DpSize,
    toppingBaseSize: DpSize,
    positionZ: Int,
): ToppingTransform {
    val canvasWidth = canvasSize.width.value
    val canvasHeight = canvasSize.height.value
    val longerBaseSide = maxOf(toppingBaseSize.width.value, toppingBaseSize.height.value)

    return ToppingTransform(
        positionX = ((offsetX.value + toppingBaseSize.width.value / 2) / canvasWidth).toDouble(),
        positionY = ((offsetY.value + toppingBaseSize.height.value / 2) / canvasHeight).toDouble(),
        positionZ = positionZ,
        scale = (longerBaseSide * scale / (canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO)).toDouble(),
        rotation = rotationDegrees.toDouble(),
    )
}
