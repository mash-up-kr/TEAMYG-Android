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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.min

/**
 * 감지된 객체를 dashed Rectangle 로 표시하고, 그 바깥 영역만 어둡게 덮는 오버레이.
 *
 * [bounds] 는 원본 이미지의 픽셀 좌표라서, 이미지가 [androidx.compose.ui.layout.ContentScale.Fit]
 * 으로 그려진 위치에 맞춰 변환한 뒤 그린다. 따라서 이미지와 **같은 크기의 영역**에 겹쳐 놓아야 한다.
 *
 * @param imageWidth 원본 이미지 가로 픽셀 수
 * @param imageHeight 원본 이미지 세로 픽셀 수
 */
@Composable
internal fun SegmentationSubjectHighlight(
    bounds: SegmentationBounds,
    imageWidth: Int,
    imageHeight: Int,
    onClickSubject: () -> Unit,
    modifier: Modifier = Modifier,
    borderWidth: Dp = SegmentationHighlightDefaults.BorderWidth,
    dashLength: Dp = SegmentationHighlightDefaults.DashLength,
    dashGap: Dp = SegmentationHighlightDefaults.DashGap,
) {
    Canvas(
        modifier = modifier.pointerInput(bounds, imageWidth, imageHeight) {
            detectTapGestures { tapOffset ->
                val rect = subjectRect(
                    bounds = bounds,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    canvasSize = size.toSize(),
                ) ?: return@detectTapGestures

                // 객체 영역 안쪽을 눌렀을 때만 반응한다
                if (rect.contains(tapOffset)) onClickSubject()
            }
        },
    ) {
        val rect = subjectRect(
            bounds = bounds,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            canvasSize = size,
        ) ?: return@Canvas

        // Difference 로 잘라내서 객체 영역 안쪽에는 딤이 칠해지지 않게 한다
        clipRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            clipOp = ClipOp.Difference,
        ) {
            drawRect(color = YGAtomicColors.Transparency.Black25)
        }

        drawRect(
            color = YGAtomicColors.Gray.Gray500,
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

/**
 * 원본 이미지 픽셀 좌표인 [bounds] 를 [androidx.compose.ui.layout.ContentScale.Fit] 으로 그려진
 * 화면 좌표로 옮긴다. 그리기와 터치 판정이 같은 계산을 쓰도록 한 곳에 모아둔다.
 *
 * @return 이미지 크기가 유효하지 않으면 null
 */
private fun subjectRect(
    bounds: SegmentationBounds,
    imageWidth: Int,
    imageHeight: Int,
    canvasSize: Size,
): Rect? {
    if (imageWidth <= 0 || imageHeight <= 0) return null

    val scale = min(canvasSize.width / imageWidth, canvasSize.height / imageHeight)
    val offsetX = (canvasSize.width - imageWidth * scale) / 2f
    val offsetY = (canvasSize.height - imageHeight * scale) / 2f

    return Rect(
        left = offsetX + bounds.left * scale,
        top = offsetY + bounds.top * scale,
        right = offsetX + bounds.right * scale,
        bottom = offsetY + bounds.bottom * scale,
    )
}

internal object SegmentationHighlightDefaults {
    val BorderWidth: Dp = 2.dp
    val DashLength: Dp = 6.dp
    val DashGap: Dp = 4.dp
}

@YGPreview
@Composable
private fun SegmentationSubjectHighlightPreview() = PreviewBox {
    SegmentationSubjectHighlight(
        bounds = SegmentationBounds(left = 80, top = 120, right = 320, bottom = 480),
        imageWidth = 400,
        imageHeight = 600,
        onClickSubject = {},
        modifier = Modifier.fillMaxSize(),
    )
}
