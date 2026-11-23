package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasEmptyMessage
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.model.CANVAS_EMPTY_INTRO_IMAGES
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroState
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_TEXT
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.IMAGE_OFFSET_DP
import kotlin.math.roundToInt

/** 이미지를 먼저 깔고 그 위에 안내 문구를 올린다 — 문구가 이미지에 가려 읽히지 않으면 안 된다 */
@Composable
internal fun CanvasEmptyIntroLayer(
    state: CanvasEmptyIntroState,
    message: String,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        CANVAS_EMPTY_INTRO_IMAGES.forEachIndexed { index, image ->
            val density = LocalDensity.current
            val centerX = constraints.maxWidth * image.centerXFraction
            val centerY = constraints.maxHeight * image.centerYFraction
            val halfWidth = with(density) { image.width.toPx() } / 2
            val halfHeight = with(density) { image.height.toPx() } / 2

            Image(
                painter = painterResource(image.res),
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset((centerX - halfWidth).roundToInt(), (centerY - halfHeight).roundToInt()) }
                    // 이동은 바깥 레이어, 회전은 안쪽 레이어 — 기울어진 이미지도 이동은 화면 기준 수직이다
                    .graphicsLayer {
                        val progress = state.imageProgress(index)
                        translationY = -IMAGE_OFFSET_DP.dp.toPx() * (1f - progress)
                        alpha = progress * state.exitAlpha
                    }.graphicsLayer { rotationZ = image.rotationDegrees }
                    .size(image.width, image.height)
                    .clearAndSetSemantics {},
            )
        }

        YGCanvasEmptyMessage(text = highlightedMessage(message) { index -> state.highlight(index) })
    }
}

private fun highlightedMessage(
    message: String,
    highlight: (charIndex: Int) -> Float,
): AnnotatedString = buildAnnotatedString {
    append(message)

    val start = message.indexOf(HIGHLIGHT_TEXT)
    if (start >= 0) {
        var charIndex = 0
        HIGHLIGHT_TEXT.forEachIndexed { offset, char ->
            if (char != ' ') {
                val color: Color = lerp(YGAtomicColors.Gray.Gray500, YGAtomicColors.Gray.Black, highlight(charIndex))
                addStyle(SpanStyle(color = color), start + offset, start + offset + 1)
                charIndex++
            }
        }
    }
}

private class CanvasEmptyIntroLayerPreviewParameterProvider : NamedPreviewParameterProvider<CanvasEmptyIntroPhase>() {
    override val entries: List<Pair<String, CanvasEmptyIntroPhase>> = listOf(
        "shown" to CanvasEmptyIntroPhase.Shown,
        "dismissed" to CanvasEmptyIntroPhase.Dismissed,
    )
}

@YGPreview
@Composable
private fun CanvasEmptyIntroLayerPreview(
    @PreviewParameter(CanvasEmptyIntroLayerPreviewParameterProvider::class) phase: CanvasEmptyIntroPhase,
) = PreviewBox {
    Box(
        modifier = Modifier
            .size(width = 360.dp, height = 480.dp)
            .background(YGAtomicColors.Gray.Gray100),
    ) {
        CanvasEmptyIntroLayer(
            state = remember(phase) { CanvasEmptyIntroState { phase } },
            message = stringResource(R.string.canvas_main_empty_message),
        )
    }
}
