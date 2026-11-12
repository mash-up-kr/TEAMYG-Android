package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasTextToppingVO
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions

private val FRAME_DURATION_NANOS = 1_000_000_000L / VideoTimelineOptions.FRAMES_PER_SECOND

/**
 * 저장 전 타임랩스를 화면에서 **반복 재생**하는 미리보기.
 *
 * @param retryKey [CanvasVideoBackground] 의 `retryKey` 와 같다
 */
@Composable
internal fun CanvasVideoPreview(
    toppings: List<CanvasToppingVO>,
    texts: List<CanvasTextToppingVO>,
    background: YGCanvasBackground?,
    retryKey: Int,
    modifier: Modifier = Modifier,
) {
    var frameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val elapsedFrames = (now - startNanos) / FRAME_DURATION_NANOS
                frameIndex = (elapsedFrames % VideoTimelineOptions.FRAME_COUNT).toInt()
            }
        }
    }

    // 캔버스 밖으로 나간 토핑은 영상(녹화 레이어)과 같이 경계에서 잘려 보여야 한다
    Box(modifier = modifier.clipToBounds()) {
        CanvasVideoBackground(
            background = background,
            retryKey = retryKey,
            modifier = Modifier.matchParentSize(),
        )

        CanvasToppingLayer(
            toppings = toppings,
            texts = texts,
            spotlightedKey = null,
            onClickTopping = {},
            onClickText = {},
            onClickSpotlightDim = {},
            hitTestEnabled = false,
            revealTogether = false,
            retryKey = retryKey,
            videoFrameIndex = frameIndex,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
