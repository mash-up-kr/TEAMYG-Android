package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.ui.reveal.rememberBatchRevealState
import com.teamyg.parfait.core.ui.reveal.revealed
import com.teamyg.parfait.core.util.android.extension.centeredAt
import com.teamyg.parfait.core.util.android.extension.toColorOrNull
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.TOPPING_BASE_LONG_SIDE_RATIO
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.canvasLoadState
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
 * 옮기고, 그 바로 아래에 나머지 전체를 덮는 Dim 레이어를 끼워 넣는다. "Spotlight 토핑 →
 * Dim 레이어 → 나머지 토핑" 은 **그리는 순서일 뿐 클릭 경로가 아니다** — 딤은 클릭을 받지
 * 않고, 판정은 전면에 깔린 입력 레이어가 혼자 한다.
 *
 * **포인터 계약**: 이 레이어가 캔버스 영역의 포인터를 독점한다. 어떤 토핑에도 맞지 않는
 * 탭이어도 이벤트는 여기서 소비되고 아래로 흐르지 않는다. 앞으로 캔버스 아래쪽에 제스처를
 * 붙이려면 이 레이어를 거쳐야 한다 — 그냥 달면 조용히 죽는다.
 *
 * @param hitTestEnabled 끄면 판정도 마스크 로딩도 달지 않는다. 클릭을 쓰지 않는 화면이
 *   쓰지도 않을 디코딩과 보이지 않는 이벤트 싱크를 떠안지 않게 하는 스위치다
 */
@Composable
internal fun CanvasToppingLayer(
    toppings: List<CanvasToppingVO>,
    spotlightedToppingId: ParfaitImageId?,
    onClickTopping: (CanvasToppingVO) -> Unit,
    onClickSpotlightDim: () -> Unit,
    modifier: Modifier = Modifier,
    hitTestEnabled: Boolean = true,
    revealTogether: Boolean = true,
    revealResetKey: Any? = Unit,
    retryKey: Int = 0,
    onLoadStateChange: (CanvasLoadState) -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier) {
        // 안쪽 Box 의 BoxScope 가 BoxWithConstraintsScope 를 가려 maxWidth 를 못 읽는다
        val areaWidth = maxWidth
        val areaHeight = maxHeight

        val entries = rememberToppingHitEntries(
            toppings = toppings,
            canvasWidth = maxWidth,
            canvasHeight = maxHeight,
            loadMasks = hitTestEnabled,
            retryKey = retryKey,
        )
        val spotlighted = entries.firstOrNull { it.topping.parfaitImageId == spotlightedToppingId }

        // 날짜를 바꿔도 이 레이어는 컴포지션에 남으므로 resetKey 없이는 빗장이 풀린 채다.
        // 다시 시도할 때도 처음부터 모아야 한다
        val reveal = rememberBatchRevealState(
            settled = entries.map { it.imageState != CanvasLoadState.Loading },
            resetKey = revealResetKey to retryKey,
        )
        val toppingsVisible = !revealTogether || reveal.shown

        val loadState = canvasLoadState(entries.map { it.imageState })
        val currentOnLoadStateChange by rememberUpdatedState(onLoadStateChange)

        LaunchedEffect(loadState) {
            currentOnLoadStateChange(loadState)
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .revealed(toppingsVisible),
        ) {
            entries.forEach { entry ->
                if (entry.topping.parfaitImageId != spotlightedToppingId) {
                    CanvasTopping(
                        entry = entry,
                        canvasWidth = areaWidth,
                        canvasHeight = areaHeight,
                        onClick = { onClickTopping(entry.topping) },
                        clickable = hitTestEnabled,
                    )
                }
            }

            if (spotlighted != null) {
                val dismissDescription = stringResource(R.string.canvas_spotlight_dismiss)

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(YGAtomicColors.Transparency.Black50)
                        // 딤 탭으로 강조를 푸는 일은 판정 오버레이의 onMiss 가 한다. 그 오버레이는
                        // pointerInput 뿐이라 시맨틱이 없으니, 접근성 서비스가 쓸 등가 액션을 여기 남긴다
                        .semantics {
                            role = Role.Button
                            contentDescription = dismissDescription
                            onClick {
                                onClickSpotlightDim()
                                true
                            }
                        },
                )

                CanvasTopping(
                    entry = spotlighted,
                    canvasWidth = areaWidth,
                    canvasHeight = areaHeight,
                    onClick = { onClickTopping(spotlighted.topping) },
                    clickable = hitTestEnabled,
                )
            }
        }

        // 드러나기 전에는 판정을 달지 않는다 — 보이지 않는 토핑이 눌리면 안 된다
        if (hitTestEnabled && toppingsVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .toppingTapInput(
                        // 강조된 토핑이 있으면 그것만 본다. 딤이 전면을 덮어 나머지는 닿지 않는다
                        entries = {
                            (spotlighted?.let(::listOf) ?: entries).map { it.topping to it.target }
                        },
                        keyOf = { it.parfaitImageId },
                        onHit = onClickTopping,
                        onMiss = { if (spotlighted != null) onClickSpotlightDim() },
                    ),
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
 *
 * @param clickable 판정을 끈 화면에서는 눌러도 아무 일이 없으므로 버튼으로 안내하지 않는다
 */
@Composable
private fun CanvasTopping(
    entry: ToppingHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
    clickable: Boolean,
) {
    val transform = entry.topping.transform
    val side = toppingLongSide(canvasWidth = canvasWidth, scale = transform.scale.toFloat())
    val description = stringResource(R.string.canvas_topping_content_description)

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
            .then(
                // 판정은 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
                if (clickable) {
                    Modifier.semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = description
                        onClick {
                            onClick()
                            true
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        ToppingImage(
            painter = entry.painter,
            border = entry.topping.border,
        )
    }
}

@Composable
private fun ToppingImage(
    painter: AsyncImagePainter,
    border: ToppingBorder,
) {
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

/**
 * 다시 시도할 때는 캐시를 건너뛴다. painter 를 새로 만들어도 디스크에 앉은 깨진 바이트를
 * 그대로 읽으면 몇 번을 눌러도 같은 실패다.
 */
@Composable
private fun retryableImageRequest(
    url: String?,
    retryKey: Int,
): ImageRequest {
    val context = LocalPlatformContext.current

    return remember(url, retryKey) {
        ImageRequest
            .Builder(context)
            .data(url)
            .apply {
                if (retryKey > 0) {
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }.build()
    }
}

internal data class ToppingHitEntry(
    val topping: CanvasToppingVO,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val target: ToppingHitTarget,
    val imageState: CanvasLoadState,
)

/**
 * 그리기와 판정이 같은 painter 를 본다. 각각 만들면 비율이 서로 다른 시점의 값이 될 수 있다.
 *
 * @param loadMasks 클릭을 받지 않는 화면은 꺼서 쓰지도 않을 디코딩을 막는다
 * @param retryKey 올리면 painter 를 새로 만든다. 같은 url 로 다시 그리기만 하면 실패한
 *   painter 가 그대로 남아 재요청이 나가지 않는다. 알파 마스크는 여기 딸려 오지 않는다
 */
@Composable
private fun rememberToppingHitEntries(
    toppings: List<CanvasToppingVO>,
    canvasWidth: Dp,
    canvasHeight: Dp,
    loadMasks: Boolean,
    retryKey: Int,
): List<ToppingHitEntry> {
    val masks = rememberToppingAlphaMasks(
        if (loadMasks) toppings.map { it.imageUrl } else emptyList(),
    )
    val density = LocalDensity.current

    return toppings.map { topping ->
        key(topping.parfaitImageId.value, retryKey) {
            val painter = rememberAsyncImagePainter(
                model = retryableImageRequest(topping.imageUrl, retryKey),
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
                imageState = when (painterState) {
                    is AsyncImagePainter.State.Success -> CanvasLoadState.Loaded
                    is AsyncImagePainter.State.Error -> CanvasLoadState.Failed
                    else -> CanvasLoadState.Loading
                },
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
