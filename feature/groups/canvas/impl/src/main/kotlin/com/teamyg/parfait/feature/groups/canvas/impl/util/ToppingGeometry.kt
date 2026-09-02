package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.feature.groups.canvas.impl.model.ToppingCorners
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * `scale = 1.0` 일 때 토핑의 긴 변이 갖는 크기. Canvas-Area **너비** 기준이다(CAN-007 §3.3).
 * 짧은 변은 원본 비율을 따라간다.
 *
 * 캔버스 메인과 편집 화면이 같은 값을 봐야 한다 — 한쪽만 바꾸면 편집에서 맞춰 놓은 크기가
 * 돌아온 캔버스에서 달라진다.
 */
const val TOPPING_BASE_LONG_SIDE_RATIO = 0.4f

fun toppingLongSide(
    canvasWidth: Dp,
    scale: Float,
): Dp = canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO * scale

/**
 * 0~1 로 정규화된 배치를 캔버스 안의 절대 Dp 좌표로 옮긴다. 좌상단이 아니라 **중심**이다 —
 * 저장된 positionX/positionY 가 그 뜻이다.
 */
fun toppingCenter(
    canvasWidth: Dp,
    canvasHeight: Dp,
    positionX: Float,
    positionY: Float,
): DpOffset = DpOffset(
    x = canvasWidth * positionX,
    y = canvasHeight * positionY,
)

/**
 * 긴 변이 [longSide]가 되도록 원본 비율([aspectRatio] = 가로÷세로)을 편 크기.
 *
 * 비율을 아직 모르면(0 이하) 정사각으로 둔다 — 그림이 뜨기 전에 크기를 지어내면 뜬 뒤에 튄다.
 */
fun toppingImageSize(
    longSide: Dp,
    aspectRatio: Float,
): DpSize = when {
    aspectRatio <= 0f -> DpSize(longSide, longSide)
    aspectRatio >= 1f -> DpSize(longSide, longSide / aspectRatio)
    else -> DpSize(longSide * aspectRatio, longSide)
}

private val STROKE_MARGIN_HORIZONTAL = SizeTokens.Size8.getDp()
private val STROKE_MARGIN_VERTICAL = SizeTokens.Size10.getDp()

/** 모서리 버튼(YGCircleButtonType.Small, 28dp)의 시각적 반지름. 버튼 중심을 스트로크 모서리에서 얼마나 밀어내야 실제로 [BUTTON_CORNER_GAP]만큼 떨어져 보이는지 계산하는 데 쓰인다. */
private val BUTTON_VISUAL_RADIUS = 14.dp

/** 모서리 버튼의 가장자리와 스트로크 사이에 실제로 보여야 하는 간격. */
private val BUTTON_CORNER_GAP = 7.dp

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
 * 크기조절 핸들을 [dragDelta]만큼 끌었을 때 배율에 곱할 값. [handleVector]는 토핑 중심에서 그
 * 핸들까지의 벡터이며, 둘은 같은 단위이어야 한다.
 *
 * 배율에 고정량을 더하면 같은 손동작이 원본이 큰 사진에서 훨씬 크게 먹는다(#383).
 */
fun resizeScaleFactor(
    handleVector: Offset,
    dragDelta: Offset,
): Float {
    val radiusSquared = handleVector.getDistanceSquared()
    if (radiusSquared <= 0f) return 1f

    val radialComponent = handleVector.x * dragDelta.x + handleVector.y * dragDelta.y
    // 한 번에 중심 너머까지 끌면 음수가 된다 — 뒤집힌 토핑 대신 0에서 멈춘다
    return (1f + radialComponent / radiusSquared).coerceAtLeast(0f)
}

/**
 * 회전 핸들을 [dragDelta]만큼 끌었을 때 늘어나는 각도. [handleVector]는 토핑 중심에서 그 핸들까지의
 * 벡터이며, 둘은 같은 단위이어야 한다.
 *
 * 드래그의 가로 성분만 보면 핸들이 놓인 모서리에 따라 회전 방향이 뒤집힌다(#383).
 */
fun rotationDeltaDegrees(
    handleVector: Offset,
    dragDelta: Offset,
): Float {
    val radiusSquared = handleVector.getDistanceSquared()
    if (radiusSquared <= 0f) return 0f

    // 화면 좌표계(y 아래)에서 시계방향 접선은 (-y, x) 이고, 그 방향 성분을 반지름으로 나누면 라디안이다
    val tangentComponent = handleVector.x * dragDelta.y - handleVector.y * dragDelta.x
    return Math.toDegrees((tangentComponent / radiusSquared).toDouble()).toFloat()
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
