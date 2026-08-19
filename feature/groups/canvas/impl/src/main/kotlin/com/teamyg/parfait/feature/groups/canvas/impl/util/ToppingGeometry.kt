package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val STROKE_MARGIN_HORIZONTAL = SizeTokens.Size8.getDp()
private val STROKE_MARGIN_VERTICAL = SizeTokens.Size10.getDp()

/** 모서리 버튼(YGCircleButtonType.Small, 28dp)의 시각적 반지름. 버튼 중심을 스트로크 모서리에서 얼마나 밀어내야 실제로 [BUTTON_CORNER_GAP]만큼 떨어져 보이는지 계산하는 데 쓰인다. */
private val BUTTON_VISUAL_RADIUS = 14.dp

/** 모서리 버튼의 가장자리와 스트로크 사이에 실제로 보여야 하는 간격. */
private val BUTTON_CORNER_GAP = 7.dp

/** 어떤 사각형(토핑 자신 또는 그 스트로크)의 회전이 반영된 네 꼭짓점. 캔버스 기준 절대 좌표. */
data class ToppingCorners(
    val topLeft: DpOffset,
    val topRight: DpOffset,
    val bottomLeft: DpOffset,
    val bottomRight: DpOffset,
)

/**
 * 선택 시 보이는 스트로크·버튼이 놓이는 네 꼭짓점. 토핑의 실제 가장자리에 여백
 * ([STROKE_MARGIN_HORIZONTAL]/[STROKE_MARGIN_VERTICAL])을 두른 사각형이며, 토핑과 함께 회전한다.
 * 스트로크는 이 사각형을 그대로 그려서 회전시키고, 버튼은 이 꼭짓점에 위치만 맞추고 자신은 회전시키지 않는다.
 */
fun computeToppingStrokeCorners(
    center: DpOffset,
    sizeAfterScale: DpSize,
    rotationDegrees: Float,
): ToppingCorners {
    val halfWidth = sizeAfterScale.width.value / 2f + STROKE_MARGIN_HORIZONTAL.value
    val halfHeight = sizeAfterScale.height.value / 2f + STROKE_MARGIN_VERTICAL.value
    return rotatedRectangleCorners(center, halfWidth, halfHeight, rotationDegrees)
}

/** [computeToppingStrokeCorners]와 같은 사각형의, 회전하기 전(로컬) 가로/세로 크기. */
fun toppingStrokeSize(sizeAfterScale: DpSize): DpSize = DpSize(
    width = sizeAfterScale.width + STROKE_MARGIN_HORIZONTAL * 2,
    height = sizeAfterScale.height + STROKE_MARGIN_VERTICAL * 2,
)

/**
 * 모서리 버튼이 놓이는 네 지점. [computeToppingStrokeCorners]의 스트로크 모서리와 겹치지 않도록,
 * 그 모서리의 대각선 방향으로 [BUTTON_CORNER_GAP]만큼 더 밀어낸 위치다 — 가로/세로에 같은 고정값을
 * 더하면 정사각형이 아닌 토핑에서 버튼이 대각선에서 벗어나므로, 반드시 대각선 방향 비율로 나눠 밀어낸다.
 */
fun computeToppingButtonPoints(
    center: DpOffset,
    sizeAfterScale: DpSize,
    rotationDegrees: Float,
): ToppingCorners {
    val halfWidth = sizeAfterScale.width.value / 2f + STROKE_MARGIN_HORIZONTAL.value
    val halfHeight = sizeAfterScale.height.value / 2f + STROKE_MARGIN_VERTICAL.value
    val diagonalLength = sqrt(halfWidth * halfWidth + halfHeight * halfHeight)
    val push = BUTTON_VISUAL_RADIUS.value + BUTTON_CORNER_GAP.value
    val pushedHalfWidth = halfWidth + push * (halfWidth / diagonalLength)
    val pushedHalfHeight = halfHeight + push * (halfHeight / diagonalLength)
    return rotatedRectangleCorners(center, pushedHalfWidth, pushedHalfHeight, rotationDegrees)
}

/**
 * 회전하지 않은 기준 방향(우측 상단 대각선)을 [rotationDegrees]만큼(시계방향) 같이 돌린 단위 벡터.
 * 크기조절 핸들이 우측 상단 모서리에 있어, 이 방향으로 드래그하면 커지고 반대 방향이면 작아진다.
 */
fun resizeOutwardDirection(rotationDegrees: Float): Pair<Float, Float> {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosT = cos(radians).toFloat()
    val sinT = sin(radians).toFloat()
    val base = 1f / sqrt(2f)

    val rotatedX = base * cosT - (-base) * sinT
    val rotatedY = base * sinT + (-base) * cosT

    return rotatedX to rotatedY
}

/** [center]를 기준으로 반너비 [halfWidth], 반높이 [halfHeight]인 사각형을 [rotationDegrees]만큼(시계방향) 돌린 네 꼭짓점. */
private fun rotatedRectangleCorners(
    center: DpOffset,
    halfWidth: Float,
    halfHeight: Float,
    rotationDegrees: Float,
): ToppingCorners {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosT = cos(radians).toFloat()
    val sinT = sin(radians).toFloat()

    fun corner(
        dx: Float,
        dy: Float,
    ): DpOffset {
        val rotatedX = dx * cosT - dy * sinT
        val rotatedY = dx * sinT + dy * cosT
        return DpOffset(center.x + rotatedX.dp, center.y + rotatedY.dp)
    }

    return ToppingCorners(
        topLeft = corner(-halfWidth, -halfHeight),
        topRight = corner(halfWidth, -halfHeight),
        bottomLeft = corner(-halfWidth, halfHeight),
        bottomRight = corner(halfWidth, halfHeight),
    )
}
