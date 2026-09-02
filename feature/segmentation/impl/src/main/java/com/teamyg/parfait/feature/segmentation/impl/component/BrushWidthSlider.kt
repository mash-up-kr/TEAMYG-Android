package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private val TRACK_HEIGHT = 2.dp
private val THUMB_SIZE = 16.dp

/**
 * 브러시 굵기를 조절하는 슬라이더.
 *
 * Material 기본 트랙은 stop indicator 와 gap 이 함께 그려져 디자인과 어긋나므로,
 * 트랙과 thumb 을 직접 그려 지나온 구간과 남은 구간만 색으로 가른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrushWidthSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = isEnabled,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .background(
                        color = YGAtomicColors.Gray.Gray850,
                        shape = YGTheme.shapes.radius.round,
                    ),
            )
        },
        track = { sliderState ->
            // 지나온 구간을 draw 단계에서 그린다. 값을 여기서 읽어야 드래그 중 recomposition 없이
            // 다시 그리기만 한다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .drawBehind {
                        drawRect(color = YGAtomicColors.Gray.Gray100)
                        drawRect(
                            color = YGAtomicColors.Gray.Gray850,
                            size = size.copy(width = size.width * sliderState.passedFraction),
                        )
                    },
            )
        },
    )
}

/** 트랙 전체에서 지나온 구간이 차지하는 비율 */
@OptIn(ExperimentalMaterial3Api::class)
private val SliderState.passedFraction: Float
    get() {
        val span = valueRange.endInclusive - valueRange.start
        return if (span == 0f) 0f else ((value - valueRange.start) / span).coerceIn(0f, 1f)
    }

private class BrushWidthSliderPreviewParameterProvider : PreviewParameterProvider<Float> {
    override val values: Sequence<Float> = sequenceOf(0f, 0.35f, 1f)
}

@YGPreview
@Composable
private fun BrushWidthSliderPreview(@PreviewParameter(BrushWidthSliderPreviewParameterProvider::class) value: Float) =
    PreviewBox {
        Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
            BrushWidthSlider(
                value = value,
                onValueChange = {},
                valueRange = 0f..1f,
            )
            BrushWidthSlider(
                value = value,
                onValueChange = {},
                valueRange = 0f..1f,
                isEnabled = false,
            )
        }
    }
