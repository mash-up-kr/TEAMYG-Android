package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.image.rememberReloadableImageRequest
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.canvasLoadState

/**
 * 타임랩스 녹화용 캔버스 한 벌. **화면에는 나타나지 않는다** — `drawContent` 를 레이어에 기록만
 * 하고 `drawLayer` 를 부르지 않는다. 저장 미리보기가 녹화하는 동안에만 붙인다.
 *
 * ⚠️ 배경을 토핑 아래에 직접 그린다. 빼면 빈 자리가 알파 0으로 기록돼 **영상이 검정**으로 나온다.
 *
 * ⚠️ [canvasWidth]·[canvasHeight] 는 캔버스 메인의 Canvas-Area 와 같은 dp 여야 한다. 토핑 테두리
 * 굵기가 화면 기준 dp 고정이라(ADR-0025) dp 폭이 달라지면 테두리가 이미지 저장물과 다른 두께로
 * 앉는다. 출력 픽셀 크기는 [Density] 를 덮어써서 맞춘다(ADR-0035).
 *
 * @param background 캔버스 메인이 `YGCanvas` 에 넘긴 것과 같은 값
 * @param retryKey 캔버스 메인과 같은 값. 0 이면 재시도 뒤에도 캐시에 앉은 옛 실패 바이트를 읽는다
 * @param onLoadStateChange 배경과 토핑을 **접은** 결과. 녹화가 이 신호 하나로 첫 프레임 시점을 정한다
 */
@Composable
internal fun CanvasVideoRecordLayer(
    toppings: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
    background: YGCanvasBackground?,
    visibleToppingCount: Int,
    canvasWidth: Dp,
    canvasHeight: Dp,
    targetWidthPx: Int,
    retryKey: Int,
    captureLayer: GraphicsLayer,
    onLoadStateChange: (CanvasLoadState) -> Unit,
) {
    val recordDensity = Density(density = targetWidthPx / canvasWidth.value)

    // 배경이 색이거나 미설정이면 기다릴 것이 없다 — 곧바로 Loaded 로 선다
    var backgroundState by remember(background, retryKey) {
        mutableStateOf(
            if (background is YGCanvasBackground.Image) CanvasLoadState.Loading else CanvasLoadState.Loaded,
        )
    }
    var toppingState by remember { mutableStateOf(CanvasLoadState.Loading) }
    val currentOnLoadStateChange by rememberUpdatedState(onLoadStateChange)

    // 배경과 토핑은 서로 다른 곳에서 결말나므로 화면 경로와 같은 규칙으로 여기서 하나로 접는다
    LaunchedEffect(backgroundState, toppingState) {
        currentOnLoadStateChange(canvasLoadState(listOf(backgroundState, toppingState)))
    }

    CompositionLocalProvider(LocalDensity provides recordDensity) {
        Box(
            modifier = Modifier
                // 부모 constraints 로 clamp 되면 해상도가 조용히 줄어든다
                .requiredSize(width = canvasWidth, height = canvasHeight)
                .drawWithContent {
                    // drawLayer 를 부르지 않는다 — 기록만 하고 화면에는 내보내지 않는다
                    captureLayer.record { this@drawWithContent.drawContent() }
                },
        ) {
            when (background) {
                // 배경을 안 고른 캔버스에도 토핑은 흰 바탕 위에 깔린다
                null -> Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = YGAtomicColors.Gray.White),
                )

                is YGCanvasBackground.Solid -> Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = background.color),
                )

                is YGCanvasBackground.Image -> AsyncImage(
                    model = rememberReloadableImageRequest(background.url, retryKey),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        backgroundState = when (state) {
                            is AsyncImagePainter.State.Success -> CanvasLoadState.Loaded
                            is AsyncImagePainter.State.Error -> CanvasLoadState.Failed
                            else -> CanvasLoadState.Loading
                        }
                    },
                    modifier = Modifier.matchParentSize(),
                )
            }

            CanvasToppingLayer(
                toppings = toppings,
                texts = texts,
                spotlightedKey = null,
                onClickTopping = {},
                onClickText = {},
                onClickSpotlightDim = {},
                hitTestEnabled = false,
                // 차례로 등장하는 것이 이 영상의 전부다. 한꺼번에 드러내는 빗장을 걸면 안 된다
                revealTogether = false,
                retryKey = retryKey,
                visibleToppingCount = visibleToppingCount,
                onLoadStateChange = { toppingState = it },
                modifier = Modifier.requiredSize(width = canvasWidth, height = canvasHeight),
            )
        }
    }
}
