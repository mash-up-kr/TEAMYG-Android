package com.teamyg.parfait.core.util.android.extension

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

fun Modifier.drawTooltipCornerTopRight(
    borderColor: Color,
    backgroundColor: Color,
    cornerWidth: Dp,
    cornerHeight: Dp,
    endPadding: Dp,
    borderWidth: Dp,
) = drawWithContent {
    drawContent()
    val triangleStart = Offset(
        x = size.width - cornerWidth.toPx() - endPadding.toPx(),
        y = borderWidth.toPx(),
    )
    val triangleEnd = Offset(
        x = size.width - endPadding.toPx(),
        y = borderWidth.toPx(),
    )
    val trianglePoint = Offset(
        x = size.width - (cornerWidth.toPx() / 2) - endPadding.toPx(),
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
