package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
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
            val span = sliderState.valueRange.endInclusive - sliderState.valueRange.start
            val passedFraction = if (span == 0f) {
                0f
            } else {
                ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TRACK_HEIGHT)
                    .background(color = YGAtomicColors.Gray.Gray100),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(passedFraction)
                        .fillMaxHeight()
                        .background(color = YGAtomicColors.Gray.Gray850),
                )
            }
        },
    )
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
