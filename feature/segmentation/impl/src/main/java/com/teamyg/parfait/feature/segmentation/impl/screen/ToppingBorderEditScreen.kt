package com.teamyg.parfait.feature.segmentation.impl.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.color
import com.teamyg.parfait.feature.segmentation.impl.editor.forEachOutlineOffset
import com.teamyg.parfait.feature.segmentation.impl.editor.forEachOutsetOutermostFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 테두리 탭의 내용. 잘라낸 알맹이에 [borderLayers] 를 겹겹이 둘러 보여준다.
 *
 * 마스크를 직접 고치는 영역 탭과 달리 실루엣 바깥으로 색을 넓히는 작업이라 비트맵은 건드리지 않는다.
 * 알맹이는 원본 자리를 지키고 테두리만 바깥으로 번지며, 원본 밖으로 나간 만큼은 잘린다.
 * 저장도 같은 자리를 같은 배율로 찍고 같은 자리에서 자르므로 여기서 본 모습이 그대로 파일로 남는다.
 */
@Composable
internal fun ToppingBorderEditScreen(
    originBitmap: Bitmap,
    segmentationBitmap: Bitmap,
    strokes: List<ToppingEditStroke>,
    borderLayers: List<ToppingBorderLayer>,
    modifier: Modifier = Modifier,
) {
    // 원본 크기 비트맵을 합성하는 일이라 컴포지션을 붙잡지 않도록 밖으로 뺀다.
    // 다시 만드는 동안에는 직전 알맹이가 그대로 남아 화면이 비지 않는다
    val cutout by produceState<Bitmap?>(initialValue = null, originBitmap, segmentationBitmap, strokes) {
        value = withContext(Dispatchers.Default) {
            buildCutoutBitmap(
                originBitmap = originBitmap,
                segmentationBitmap = segmentationBitmap,
                strokes = strokes,
            )
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 윤곽은 알맹이를 겹 하나당 스물네 번 찍어 만든다. 원본 해상도로 찍으면 프레임마다 큰 그림을
    // 그만큼 읽으므로, 화면에 나올 크기로 한 번 줄여 두고 그 사본을 찍는다.
    // 줄이는 것도 원본 해상도를 훑는 일이라 알맹이를 만들 때와 마찬가지로 컴포지션 밖에서 한다
    val stampImage: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, cutout, canvasSize) {
        val source = cutout ?: return@produceState
        val mapping = BitmapViewMapping.fitCenter(canvasSize, source.width, source.height)
        val width = (source.width * mapping.scale).roundToInt()
        val height = (source.height * mapping.scale).roundToInt()
        if (width <= 0 || height <= 0) return@produceState

        value = withContext(Dispatchers.Default) { source.scale(width, height).asImageBitmap() }
    }

    Canvas(modifier = modifier.onSizeChanged { size -> canvasSize = size }) {
        val image = stampImage ?: return@Canvas
        val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)

        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(image.width, image.height)

        // 저장 결과가 원본 크기를 지키므로 밖으로 나간 테두리는 잘려 나간다.
        // 화면도 원본 자리에서 똑같이 잘라야 여기서 본 모습이 그대로 파일로 남는다
        clipRect(
            left = dstOffset.x.toFloat(),
            top = dstOffset.y.toFloat(),
            right = (dstOffset.x + dstSize.width).toFloat(),
            bottom = (dstOffset.y + dstSize.height).toFloat(),
        ) {
            // 굵기가 dp 라 화면 배율을 태우지 않는다. 사진 해상도가 달라도 눈에 보이는 굵기는 같다
            borderLayers.forEachOutsetOutermostFirst(density) { layer, layerOutset ->
                drawOutline(
                    image = image,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    radius = layerOutset,
                    color = layer.color,
                )
            }

            drawImage(image = image, dstOffset = dstOffset, dstSize = dstSize)
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
    forEachOutlineOffset(radius) { offset ->
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
        originBitmap = createBitmap(1, 1),
        segmentationBitmap = createBitmap(1, 1),
        strokes = emptyList(),
        borderLayers = emptyList(),
        modifier = Modifier.fillMaxSize(),
    )
}
