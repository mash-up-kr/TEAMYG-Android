package com.teamyg.parfait.feature.segmentation.impl.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.editor.buildEditedBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.outlineOffsets
import com.teamyg.parfait.feature.segmentation.impl.editor.withOutsets
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState
import kotlin.math.roundToInt

/**
 * 테두리 탭의 내용. 잘라낸 결과에 [ToppingEditState.borderStrokes] 를 겹겹이 둘러 보여준다.
 *
 * 마스크를 직접 고치는 영역 탭과 달리 실루엣 바깥으로 색을 넓히는 작업이라 비트맵은 건드리지 않는다.
 * 알맹이는 원본 자리를 지키고 테두리만 바깥으로 번지며, 원본 밖으로 나간 만큼은 잘린다.
 * 저장도 같은 자리를 같은 배율로 찍고 같은 자리에서 자르므로 여기서 본 모습이 그대로 파일로 남는다.
 */
@Composable
internal fun ToppingBorderEditScreen(
    state: ToppingEditState,
    modifier: Modifier = Modifier,
) {
    val originBitmap = state.originBitmap ?: return
    val segmentationBitmap = state.segmentationBitmap ?: return

    // 테두리는 잘라낸 결과의 실루엣을 따라 두르므로 영역 탭 편집까지 반영된 결과를 만들어 두고 쓴다.
    // 이 결과의 알파가 곧 실루엣이라 윤곽과 본 그림에 같은 이미지를 쓸 수 있다
    val editedImage = remember(originBitmap, segmentationBitmap, state.strokes) {
        buildEditedBitmap(
            originBitmap = originBitmap,
            segmentationBitmap = segmentationBitmap,
            strokes = state.strokes,
        ).asImageBitmap()
    }

    Canvas(modifier = modifier) {
        val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)

        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(
            width = (originBitmap.width * mapping.scale).roundToInt(),
            height = (originBitmap.height * mapping.scale).roundToInt(),
        )

        // 저장 결과가 원본 크기를 지키므로 밖으로 나간 테두리는 잘려 나간다.
        // 화면도 원본 자리에서 똑같이 잘라야 여기서 본 모습이 그대로 파일로 남는다
        clipRect(
            left = dstOffset.x.toFloat(),
            top = dstOffset.y.toFloat(),
            right = (dstOffset.x + dstSize.width).toFloat(),
            bottom = (dstOffset.y + dstSize.height).toFloat(),
        ) {
            // 겹이 안쪽부터 쌓여 있으므로 그릴 때는 가장 바깥부터 깔아야 안쪽 겹이 위에 남는다
            state.borderStrokes.withOutsets().asReversed().forEach { (stroke, strokeOutset) ->
                drawOutline(
                    image = editedImage,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    // 굵기는 원본 비트맵 기준이라 화면 배율을 태운다
                    radius = strokeOutset * mapping.scale,
                    color = stroke.color,
                )
            }

            drawImage(image = editedImage, dstOffset = dstOffset, dstSize = dstSize)
        }
    }
}

/** 알파는 두고 색만 갈아끼우는 tint 라 반투명한 가장자리도 원래 모양대로 번진다 */
private fun DrawScope.drawOutline(
    image: ImageBitmap,
    dstOffset: IntOffset,
    dstSize: IntSize,
    radius: Float,
    color: Color,
) {
    if (radius <= 0f) return

    val colorFilter = ColorFilter.tint(color)
    outlineOffsets(radius).forEach { offset ->
        drawImage(
            image = image,
            dstOffset = IntOffset(
                x = dstOffset.x + offset.x.roundToInt(),
                y = dstOffset.y + offset.y.roundToInt(),
            ),
            dstSize = dstSize,
            colorFilter = colorFilter,
        )
    }
}

@YGPreview
@Composable
private fun PreviewToppingBorderEditScreen() = PreviewBox {
    ToppingBorderEditScreen(
        state = ToppingEditState(),
        modifier = Modifier.fillMaxSize(),
    )
}
