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
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.canvasLoadState

/**
 * 타임랩스 녹화만을 위해 캔버스를 한 벌 더 띄운다. **화면에는 나타나지 않는다** —
 * `drawContent` 를 레이어에 기록만 하고 `drawLayer` 를 부르지 않는다.
 *
 * 사용자에게 보이는 캔버스를 프레임마다 흔들지 않으려고 따로 띄운다. 그쪽을 쓰면 녹화 중
 * 캔버스가 깜빡이고, 스포트라이트·판정 같은 화면 상태가 영상에 섞인다.
 *
 * ⚠️ **배경을 토핑 아래에 직접 그린다.** 이미지 저장 경로(`YGCanvas` 의 `CanvasArea`)가
 * 기록하는 것이 "배경 + 토핑"이라, 여기서 배경을 빼면 빈 자리가 알파 0으로 기록되고
 * `BitmapSurfaceWriter` 가 블렌딩 없이 텍셀을 그대로 써서 **영상이 검정**으로 나온다.
 * 그러면 도입 구간(배경만, 토핑 0개)이 검정 화면이 되고, "마지막 프레임 = 이미지 저장물"
 * 이라는 약속도 깨진다. 세 갈래(미설정·[YGCanvasBackground.Solid]·[YGCanvasBackground.Image])
 * 를 [CanvasArea][com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas] 와 같은
 * 방식으로 그린다.
 *
 * ⚠️ **[canvasWidth]·[canvasHeight] 는 화면의 Canvas-Area 와 같은 dp 여야 한다.** 토핑 테두리
 * 굵기가 화면 기준 dp 고정이라(ADR-0025), dp 폭이 달라지면 캔버스 폭에 대한 굵기 비율이 달라져
 * 영상의 테두리가 이미지 저장물과 다른 두께로 앉는다.
 *
 * 출력 픽셀 크기는 [Density] 를 덮어써서 고정한다 — dp 기하는 화면과 같게 두고 픽셀만 옮기는
 * 유일한 방법이다(`adr/0033-canvas-video-onscreen-capture-encoding.md`).
 *
 * @param background 화면이 [YGCanvas][com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvas]
 *   에 넘기는 것과 **같은 값**을 받아야 한다. 변환을 두 번 하면 어긋날 수 있다
 * @param targetWidthPx 산출물의 가로 픽셀. [canvasWidth] 와 함께 밀도를 정한다
 * @param retryKey 화면과 같은 값을 받아야 한다. 재시도 뒤 0 이면 이 레이어만 캐시에 앉은
 *   옛 실패 바이트를 읽어, 화면은 정상인데 녹화만 실패한다
 * @param onLoadStateChange 배경과 토핑을 **접은** 결과를 보고한다. 녹화 게이트가 이 신호
 *   하나만 보고 첫 프레임 시점을 정하므로, 둘 중 하나라도 빠지면 덜 그려진 프레임을 찍는다
 */
@Composable
internal fun CanvasVideoRecordLayer(
    toppings: List<CanvasToppingVO>,
    background: YGCanvasBackground?,
    visibleToppingCount: Int,
    lastToppingPopProgress: Float,
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
                spotlightedToppingId = null,
                onClickTopping = {},
                onClickSpotlightDim = {},
                hitTestEnabled = false,
                // 하나씩 등장하는 것이 이 영상의 전부다. 한꺼번에 드러내는 빗장을 걸면 안 된다
                revealTogether = false,
                retryKey = retryKey,
                visibleToppingCount = visibleToppingCount,
                lastToppingPopProgress = lastToppingPopProgress,
                onLoadStateChange = { toppingState = it },
                modifier = Modifier.requiredSize(width = canvasWidth, height = canvasHeight),
            )
        }
    }
}
