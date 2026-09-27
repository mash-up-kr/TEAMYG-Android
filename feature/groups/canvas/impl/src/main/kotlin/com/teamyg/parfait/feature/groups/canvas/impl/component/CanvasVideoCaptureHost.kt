package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 동영상 프레임을 캡처하기 위한 오프스크린 재생 자리. [YGCanvas] 의 캡처(배경+토핑만, 테두리·
 * 메뉴 같은 화면 크롬 제외)와 같은 대상을 그린다 — 호출부가 화면 밖으로 배치해 사용자에게는
 * 보이지 않는다.
 */
@Composable
internal fun CanvasVideoCaptureHost(
    background: YGCanvasBackground?,
    toppings: List<CanvasToppingVO>,
    revealProgress: Map<ParfaitImageId, Float>,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(LocalConfiguration.current.screenWidthDp.dp)
            .aspectRatio(CANVAS_AREA_ASPECT_RATIO)
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            },
    ) {
        when (background) {
            null -> Box(modifier = Modifier.matchParentSize().background(YGAtomicColors.Gray.White))
            is YGCanvasBackground.Solid -> Box(modifier = Modifier.matchParentSize().background(background.color))
            is YGCanvasBackground.Image -> AsyncImage(
                model = background.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        CanvasToppingLayer(
            toppings = toppings,
            spotlightedToppingId = null,
            onClickTopping = {},
            onClickSpotlightDim = {},
            hitTestEnabled = false,
            revealProgress = { id -> revealProgress[id] ?: 0f },
            modifier = Modifier.matchParentSize(),
        )
    }
}
