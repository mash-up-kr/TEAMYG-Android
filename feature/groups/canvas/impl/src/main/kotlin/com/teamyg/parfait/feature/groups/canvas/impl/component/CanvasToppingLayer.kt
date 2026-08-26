package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BASE_LONG_SIDE_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberToppingAlphaMasks
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingCenter
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingImageSize
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingLongSide

/**
 * 저장된 배치대로 토핑을 얹는다. [modifier] 로 Canvas-Area 와 같은 크기를 잡아 줘야 한다 —
 * 위치·크기가 모두 그 폭에 대한 비율이라 다른 크기 위에 얹으면 배치가 어긋난다.
 *
 * [toppings] 는 그리는 순서대로 받는다(positionZ 오름차순). 뒤에 오는 것이 위에 덮인다.
 *
 * Spotlight(C-106): [spotlightedToppingId] 가 있으면 그 토핑만 목록 순서를 벗어나 맨 위로
 * 옮기고, 그 바로 아래에 나머지 전체를 덮는 Dim 레이어를 끼워 넣는다 — 우선순위는
 * "Spotlight 토핑 → Dim 레이어 → 나머지 토핑 → 배경" 순이다(배경은 이 레이어 바깥,
 * 이 레이어를 감싸는 YGCanvas 가 그린다).
 */
@Composable
internal fun CanvasToppingLayer(
    toppings: List<CanvasToppingVO>,
    spotlightedToppingId: ParfaitImageId?,
    onClickTopping: (CanvasToppingVO) -> Unit,
    onClickSpotlightDim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spotlightedTopping = toppings.firstOrNull { it.parfaitImageId == spotlightedToppingId }

    BoxWithConstraints(modifier = modifier) {
        toppings.forEach { topping ->
            if (topping.parfaitImageId != spotlightedToppingId) {
                CanvasTopping(
                    topping = topping,
                    canvasWidth = maxWidth,
                    canvasHeight = maxHeight,
                    onClick = { onClickTopping(topping) },
                )
            }
        }

        if (spotlightedTopping != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(YGAtomicColors.Transparency.Black50)
                    .clickableYGNoRipple(onClick = onClickSpotlightDim),
            )

            CanvasTopping(
                topping = spotlightedTopping,
                canvasWidth = maxWidth,
                canvasHeight = maxHeight,
                onClick = { onClickTopping(spotlightedTopping) },
            )
        }
    }
}

/**
 * positionX·positionY 는 Canvas-Area 대비 0~1 로 정규화된 **중심점**이라 [centeredAt] 으로 앉힌다.
 *
 * 정사각 박스에 [ContentScale.Fit] 로 담으면 긴 변이 박스에 꽉 차고 짧은 변이 비율대로
 * 줄어들므로, 원본 크기를 몰라도 [TOPPING_BASE_LONG_SIDE_RATIO] 규칙이 지켜진다.
 *
 * 캔버스 밖으로 나간 배치도 그대로 둔다 — 되돌리거나 가장자리에 붙이지 않고, 넘친 픽셀은
 * Canvas-Area 의 clip 이 잘라 낸다(CAN-007 §3.6·§3.7).
 */
@Composable
private fun CanvasTopping(
    topping: CanvasToppingVO,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
) {
    val transform = topping.transform
    val side = toppingLongSide(canvasWidth = canvasWidth, scale = transform.scale.toFloat())

    Box(
        modifier = Modifier
            .centeredAt(
                toppingCenter(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    positionX = transform.positionX.toFloat(),
                    positionY = transform.positionY.toFloat(),
                ),
            )
            // size 는 부모 constraints 로 clamp 돼 토핑이 잘리는 대신 작아진다 — requiredSize 를 쓴다
            .requiredSize(side)
            .graphicsLayer { rotationZ = transform.rotation.toFloat() }
            .clickableYGNoRipple(onClick = onClick),
    ) {
        ToppingImage(
            imageUrl = topping.imageUrl,
            border = topping.border,
        )
    }
}

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
    val solidBorder = border as? ToppingBorder.Solid

    YGToppingCutoutImage(
        painter = painter,
        // 색을 못 읽으면 테두리를 걸러 낸다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀리다.
        // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
        borderColor = solidBorder
            ?.color
            ?.toColorOrNull()
            ?.takeIf { painterState is AsyncImagePainter.State.Success },
        borderWidth = (solidBorder?.width?.toFloat() ?: 0f).dp,
        modifier = Modifier.fillMaxSize(),
    )
}

internal data class ToppingHitEntry(
    val topping: CanvasToppingVO,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val target: ToppingHitTarget,
)

/**
 * 그리기와 판정이 같은 painter 를 본다. 각각 만들면 비율이 서로 다른 시점의 값이 될 수 있다.
 *
 * @param loadMasks 클릭을 받지 않는 화면은 꺼서 쓰지도 않을 디코딩을 막는다
 */
@Composable
private fun rememberToppingHitEntries(
    toppings: List<CanvasToppingVO>,
    canvasWidth: Dp,
    canvasHeight: Dp,
    loadMasks: Boolean,
): List<ToppingHitEntry> {
    val masks = rememberToppingAlphaMasks(
        if (loadMasks) toppings.map { it.imageUrl } else emptyList(),
    )
    val density = LocalDensity.current

    return toppings.map { topping ->
        key(topping.parfaitImageId.value) {
            val painter = rememberAsyncImagePainter(
                model = topping.imageUrl,
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()
            val intrinsicSize = painter.intrinsicSize

            val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                0f
            }

            val longSide = toppingLongSide(canvasWidth, topping.transform.scale.toFloat())
            val imageSize = toppingImageSize(longSide = longSide, aspectRatio = aspectRatio)
            val center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.transform.positionX.toFloat(),
                positionY = topping.transform.positionY.toFloat(),
            )

            // 테두리는 색을 못 읽거나 그림이 안 떴으면 그려지지 않는다 — 판정도 같은 조건이어야 한다
            val drawnBorderWidth = (topping.border as? ToppingBorder.Solid)
                ?.takeIf { it.color.toColorOrNull() != null }
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.width
                ?.toFloat()
                ?: 0f

            ToppingHitEntry(
                topping = topping,
                painter = painter,
                target = with(density) {
                    ToppingHitTarget(
                        centerXPx = center.x.toPx(),
                        centerYPx = center.y.toPx(),
                        imageWidthPx = imageSize.width.toPx(),
                        imageHeightPx = imageSize.height.toPx(),
                        rotationDegrees = topping.transform.rotation.toFloat(),
                        borderWidthPx = drawnBorderWidth.dp.toPx(),
                        mask = masks[topping.imageUrl],
                    )
                },
            )
        }
    }
}
