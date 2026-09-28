package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.extension.centeredAt
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
 *
 * ⚠️ 자리는 반드시 [centeredAt]으로 잡는다 — 캔버스보다 커지면 모서리 버튼과 벌어진다(#313).
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
            .centeredAt(center)
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
