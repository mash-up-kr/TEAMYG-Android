package com.teamyg.parfait.feature.segmentation.impl.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.SegmentationBounds

/**
 * 감지된 후보들을 dashed Rectangle 로 표시하고, **모든 후보 바깥**을 어둡게 덮는 오버레이.
 *
 * [boundsList] 는 원본 이미지의 픽셀 좌표라서, 이미지가 [androidx.compose.ui.layout.ContentScale.Fit]
 * 으로 그려진 위치에 맞춰 변환한 뒤 그린다. 따라서 이미지와 **같은 크기의 영역**에 겹쳐 놓아야 한다.
 *
 * @param imageWidth 원본 이미지 가로 픽셀 수
 * @param imageHeight 원본 이미지 세로 픽셀 수
 */
@Composable
internal fun SegmentationSubjectHighlight(
    boundsList: List<SegmentationBounds>,
    imageWidth: Int,
    imageHeight: Int,
    onClickCandidate: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    borderWidth: Dp = SegmentationHighlightDefaults.BorderWidth,
    dashLength: Dp = SegmentationHighlightDefaults.DashLength,
    dashGap: Dp = SegmentationHighlightDefaults.DashGap,
) {
    Canvas(
        modifier = modifier.pointerInput(boundsList, imageWidth, imageHeight) {
            detectTapGestures { tapOffset ->
                pickCandidateIndex(
                    boundsList = boundsList,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    tapX = tapOffset.x,
                    tapY = tapOffset.y,
                )?.let(onClickCandidate)
            }
        },
    ) {
        val rects = boundsList.mapNotNull { bounds ->
            scaledRectOrNull(
                bounds = bounds,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                canvasWidth = size.width,
                canvasHeight = size.height,
            )
        }

        if (rects.isEmpty()) return@Canvas

        // 후보를 모두 담은 하나의 Path 를 빼면, 후보 수만큼 clipRect 를 중첩한 것과 결과가 같으면서
        // 재귀 없이 평평하다
        val holes = Path().apply {
            rects.forEach { rect ->
                addRect(Rect(left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom))
            }
        }

        clipPath(path = holes, clipOp = ClipOp.Difference) {
            drawRect(color = YGAtomicColors.Transparency.Black25)
        }

        rects.forEach { rect ->
            drawRect(
                color = YGAtomicColors.Gray.White,
                topLeft = Offset(x = rect.left, y = rect.top),
                size = Size(width = rect.width, height = rect.height),
                style = Stroke(
                    width = borderWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                    ),
                ),
            )
        }
    }
}

internal object SegmentationHighlightDefaults {
    val BorderWidth: Dp = SizeTokens.Size2.getDp()
    val DashLength: Dp = SizeTokens.Size6.getDp()
    val DashGap: Dp = SizeTokens.Size4.getDp()
}

@YGPreview
@Composable
private fun SegmentationSubjectHighlightPreview() = PreviewBox {
    SegmentationSubjectHighlight(
        boundsList = listOf(
            SegmentationBounds(left = 80, top = 120, right = 320, bottom = 480),
            SegmentationBounds(left = 40, top = 40, right = 140, bottom = 110),
        ),
        imageWidth = 400,
        imageHeight = 600,
        onClickCandidate = {},
        modifier = Modifier.fillMaxSize(),
    )
}
