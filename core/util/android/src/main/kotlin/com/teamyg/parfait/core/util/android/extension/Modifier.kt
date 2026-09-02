package com.teamyg.parfait.core.util.android.extension

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private val DefaultScrollbarWidth = 2.dp
private val DefaultScrollbarEndPadding = 3.dp
private val DefaultScrollbarVerticalPadding = 4.dp

@Composable
fun Modifier.navigationBarsAndImePadding(): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarBottomPadding = WindowInsets.navigationBars
    val imeBottomPadding = WindowInsets.ime

    val insets = WindowInsets(
        left = maxOf(
            a = navigationBarBottomPadding.getLeft(density, layoutDirection),
            b = imeBottomPadding.getLeft(density, layoutDirection),
        ),
        right = maxOf(
            a = navigationBarBottomPadding.getRight(density, layoutDirection),
            b = imeBottomPadding.getRight(density, layoutDirection),
        ),
        top = maxOf(
            a = navigationBarBottomPadding.getTop(density),
            b = imeBottomPadding.getTop(density),
        ),
        bottom = maxOf(
            a = navigationBarBottomPadding.getBottom(density),
            b = imeBottomPadding.getBottom(density),
        ),
    )
    return windowInsetsPadding(insets)
}

/**
 * Compose 에는 [ScrollState] 용 스크롤바가 없어 직접 그린다. `drawBehind` 가 아니라
 * `drawWithContent` 인 이유는 막대가 내용 위에 얹혀야 해서다.
 *
 * `verticalScroll` 보다 앞에 둬야 스크롤 이동이 반영되지 않은 뷰포트 좌표에 그려, 내용이
 * 움직여도 막대가 제자리에 선다.
 */
fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    color: Color,
    width: Dp = DefaultScrollbarWidth,
    endPadding: Dp = DefaultScrollbarEndPadding,
    verticalPadding: Dp = DefaultScrollbarVerticalPadding,
): Modifier = drawWithContent {
    drawContent()
    if (scrollState.maxValue <= 0) return@drawWithContent

    val viewportHeight = size.height
    val contentHeight = viewportHeight + scrollState.maxValue

    val trackTop = verticalPadding.toPx()
    val trackHeight = viewportHeight - trackTop * 2

    // 뷰포트가 전체 내용에서 차지하는 비율이 곧 막대 길이다
    val thumbHeight = trackHeight * viewportHeight / contentHeight
    val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue
    val thumbWidth = width.toPx()

    drawRoundRect(
        color = color,
        topLeft = Offset(
            x = size.width - endPadding.toPx() - thumbWidth,
            y = trackTop + scrollRatio * (trackHeight - thumbHeight),
        ),
        size = Size(width = thumbWidth, height = thumbHeight),
        cornerRadius = CornerRadius(thumbWidth / 2),
    )
}

fun Modifier.drawTooltipCornerTop(
    borderColor: Color,
    backgroundColor: Color,
    cornerWidth: Dp,
    cornerHeight: Dp,
    borderWidth: Dp,
    arrowCenterX: DrawScope.() -> Float,
) = drawWithContent {
    drawContent()

    val halfCornerWidth = cornerWidth.toPx() / 2
    val centerX = arrowCenterX().coerceIn(
        minimumValue = halfCornerWidth,
        maximumValue = (size.width - halfCornerWidth).coerceAtLeast(halfCornerWidth),
    )

    val triangleStart = Offset(
        x = centerX - halfCornerWidth,
        y = borderWidth.toPx(),
    )
    val triangleEnd = Offset(
        x = centerX + halfCornerWidth,
        y = borderWidth.toPx(),
    )
    val trianglePoint = Offset(
        x = centerX,
        y = 0f - cornerHeight.toPx(),
    )

    // 바깥 삼각형 (테두리 색상의 삼각형)
    val outerTrianglePath = getTrianglePath(
        startPoint = triangleStart,
        endPoint = triangleEnd,
        trianglePoint = trianglePoint,
    )

    // 안쪽 삼각형이 툴팁의 테두리를 완전히 덮도록 보간한 값
    // border 가 안팎으로 커지다보니까 border size 비례하게 보간값이 증가해야함
    val innerTriangleInterpolationWidth = borderWidth.toPx() / cornerHeight.toPx() * cornerWidth.toPx()
    val innerTriangleInterpolationHeight = borderWidth.toPx()

    // 안쪽 삼각형 (선 처럼 보이기 위해 안쪽 삼각형은 배경색과 동일하게 설정)
    val innerTrianglePath = getTrianglePath(
        startPoint = triangleStart.copy(
            x = triangleStart.x + (borderWidth.toPx() * 2) - innerTriangleInterpolationWidth,
            y = triangleStart.y + innerTriangleInterpolationHeight,
        ),
        endPoint = triangleEnd.copy(
            x = triangleEnd.x - (borderWidth.toPx() * 2) + innerTriangleInterpolationWidth,
            y = triangleStart.y + innerTriangleInterpolationHeight,
        ),
        trianglePoint = trianglePoint.copy(
            y = trianglePoint.y + (borderWidth.toPx() * 2),
        ),
    )
    drawPath(outerTrianglePath, borderColor)
    drawPath(innerTrianglePath, backgroundColor)
}

private fun getTrianglePath(
    startPoint: Offset,
    endPoint: Offset,
    trianglePoint: Offset,
): Path = Path().apply {
    moveTo(startPoint.x, startPoint.y)
    lineTo(trianglePoint.x, trianglePoint.y)
    lineTo(endPoint.x, endPoint.y)
    close()
}

/** [point](부모 기준 절대 Dp 좌표)에 이 컴포저블 자신의 중심이 오도록 배치한다. */
fun Modifier.centeredAt(point: DpOffset): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val x = point.x.roundToPx() - placeable.width / 2
        val y = point.y.roundToPx() - placeable.height / 2
        placeable.placeRelative(x, y)
    }
}

/** [key]가 바뀌면 제스처를 새로 추적하며, 드래그한 만큼(픽셀) [onDrag]로 넘겨준다. */
fun Modifier.dragBy(
    key: Any?,
    onDrag: (Offset) -> Unit,
): Modifier = pointerInput(key) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        onDrag(dragAmount)
    }
}
