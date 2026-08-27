package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.core.util.android.extension.dragBy
import com.teamyg.parfait.feature.groups.canvas.impl.util.rotationDeltaDegrees
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingStrokeSize

/**
 * [painter]의 실제 가로세로 크기(배율 적용 전, 배율 1배 기준).
 * 로딩 중에는 고정값을 임시로 쓰다가, 크기를 알게 되는 즉시 실제 크기로 다시 계산된다.
 */
@Composable
internal fun rememberToppingBaseSize(painter: Painter): DpSize {
    val intrinsicSize = painter.intrinsicSize
    val density = LocalDensity.current

    return remember(intrinsicSize, density) {
        if (intrinsicSize.isSpecified && intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
            with(density) {
                DpSize(
                    width = intrinsicSize.width.toDp(),
                    height = intrinsicSize.height.toDp(),
                )
            }
        } else {
            DpSize(60.dp, 60.dp)
        }
    }
}

/**
 * 토핑과 함께 회전하는 흰색 2dp 점선 스트로크. [center]에 여백이 반영된 크기([toppingStrokeSize])로
 * 놓은 뒤 [rotationDegrees]만큼 [graphicsLayer]로 돌려, 토핑 자신의 회전을 그대로 따라가게 한다.
 */
@Composable
internal fun ToppingSelectionStroke(
    center: DpOffset,
    sizeAfterScale: DpSize,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val strokeSize = toppingStrokeSize(sizeAfterScale)

    Box(
        modifier = modifier
            .offset(x = center.x - strokeSize.width / 2, y = center.y - strokeSize.height / 2)
            .requiredSize(strokeSize)
            .graphicsLayer(rotationZ = rotationDegrees)
            .drawBehind {
                drawRect(
                    color = YGAtomicColors.Gray.White,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(7.5.dp.toPx(), 9.dp.toPx()),
                        ),
                    ),
                )
            },
    )
}

/**
 * 누르는 버튼이 아니라 잡고 끄는 핸들. 원형 아이콘 버튼 위에 드래그 제스처를 얹어서 쓴다.
 * [key]가 바뀌면 드래그 제스처를 새로 추적한다 — 토핑이 여럿이면 그 토핑의 id를, 하나뿐이면 [Unit]을 넘기면 된다.
 */
@Composable
internal fun ToppingDragHandleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    point: DpOffset,
    key: Any?,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    YGCircleButton(
        iconResource = iconRes,
        type = YGCircleButtonType.Small,
        contentDescription = contentDescription,
        onClick = {},
        modifier = modifier
            .centeredAt(point)
            .dragBy(key, onDrag),
    )
}

/** 잡고 돌리는 회전 핸들. 끈 거리를 각도로 바꿔 [onRotate]로 넘긴다. */
@Composable
internal fun ToppingRotateHandleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    point: DpOffset,
    center: DpOffset,
    key: Any?,
    onRotate: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    // 돌리는 동안 핸들도 함께 도는데 [dragBy] 의 제스처 블록은 시작 시점 람다를 계속 쓴다 —
    // State 로 읽지 않으면 처음 위치의 접선에 갇혀 한 바퀴를 못 돈다(#383).
    val handleVector by rememberUpdatedState(
        with(density) {
            Offset(x = (point.x - center.x).toPx(), y = (point.y - center.y).toPx())
        },
    )

    ToppingDragHandleButton(
        iconRes = iconRes,
        contentDescription = contentDescription,
        point = point,
        key = key,
        onDrag = { drag -> onRotate(rotationDeltaDegrees(handleVector = handleVector, dragDelta = drag)) },
        modifier = modifier,
    )
}
