package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import kotlin.math.cos
import kotlin.math.sin

/**
 * `scale = 1.0` 일 때 토핑의 긴 변이 갖는 크기. Canvas-Area 너비 기준이다(CAN-007 §3.3).
 *
 * 짧은 변은 원본 비율을 따라간다 — 정사각 박스에 [ContentScale.Fit] 로 담으면 긴 변이 박스에
 * 꽉 차고 짧은 변이 비율대로 줄어들므로, 원본 크기를 몰라도 규칙이 지켜진다.
 */
private const val TOPPING_BASE_LONG_SIDE_RATIO = 0.4f

/** 누끼 외곽선을 찍는 방향 수. 8 방향이면 대각까지 메워져 이음매가 보이지 않는다 */
private const val OUTLINE_STAMP_COUNT = 8

private const val FULL_TURN_DEGREES = 360.0

/**
 * 저장된 배치대로 토핑을 얹는다. 부모는 Canvas-Area 와 같은 크기여야 한다 —
 * 위치·크기가 모두 그 폭에 대한 비율이라 다른 크기 위에 얹으면 배치가 어긋난다.
 *
 * [toppings] 는 그리는 순서대로 받는다(positionZ 오름차순). 뒤에 오는 것이 위에 덮인다.
 */
@Composable
internal fun CanvasToppingLayer(
    toppings: List<CanvasToppingVO>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        toppings.forEach { topping ->
            CanvasTopping(
                topping = topping,
                canvasWidth = maxWidth,
                canvasHeight = maxHeight,
            )
        }
    }
}

/**
 * positionX·positionY 는 Canvas-Area 대비 0~1 로 정규화된 **중심점**이다. Compose 의 offset 은
 * 좌상단 기준이라 한 변의 절반을 빼서 옮긴다.
 *
 * 캔버스 밖으로 나간 배치도 그대로 둔다 — 되돌리거나 가장자리에 붙이지 않고, 넘친 픽셀은
 * Canvas-Area 의 clip 이 잘라 낸다(CAN-007 §3.6·§3.7).
 */
@Composable
private fun CanvasTopping(
    topping: CanvasToppingVO,
    canvasWidth: Dp,
    canvasHeight: Dp,
) {
    val transform = topping.transform
    val side = canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO * transform.scale.toFloat()

    Box(
        modifier = Modifier
            .offset(
                x = canvasWidth * transform.positionX.toFloat() - side / 2,
                y = canvasHeight * transform.positionY.toFloat() - side / 2,
            ).size(side)
            .graphicsLayer { rotationZ = transform.rotation.toFloat() },
    ) {
        ToppingImage(
            imageUrl = topping.imageUrl,
            border = topping.border,
        )
    }
}

/**
 * 누끼 이미지라 테두리도 실루엣을 따라야 한다. 사각 테두리를 두르면 잘라 낸 배경이 다시
 * 드러나므로, 같은 그림을 테두리 색으로 물들여 여덟 방향으로 밀어 찍고 그 위에 원본을 얹는다.
 *
 * 그림은 [ContentScale.Fit] 로 담아 긴 변만 박스에 맞춘다 — 원본 비율을 몰라도 CAN-007 §3.3 이
 * 지켜지고, 남는 여백은 투명이라 배치 중심도 그대로다.
 */
@Composable
private fun ToppingImage(
    imageUrl: String,
    border: ToppingBorder,
) {
    val painter = rememberAsyncImagePainter(
        model = imageUrl,
        contentScale = ContentScale.Fit,
    )
    val painterState by painter.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
        if (border is ToppingBorder.Solid && painterState is AsyncImagePainter.State.Success) {
            ToppingOutline(
                painter = painter,
                border = border,
            )
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * `borderWidth` 는 화면 기준 dp 다 — 1.0 이 1dp 이고 토핑을 키워도 굵기는 그대로다.
 *
 * 색을 못 읽으면 테두리를 걸러 낸다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀린다.
 */
@Composable
private fun ToppingOutline(
    painter: AsyncImagePainter,
    border: ToppingBorder.Solid,
) {
    val color = border.color.toColorOrNull() ?: return
    val widthPx = with(LocalDensity.current) { border.width.dp.toPx() }

    repeat(OUTLINE_STAMP_COUNT) { index ->
        val radians = Math.toRadians(FULL_TURN_DEGREES / OUTLINE_STAMP_COUNT * index)

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (cos(radians) * widthPx).toFloat()
                    translationY = (sin(radians) * widthPx).toFloat()
                },
        )
    }
}
