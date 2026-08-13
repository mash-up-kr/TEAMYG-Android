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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingOutlineDistanceField
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.toBorderBands
import com.teamyg.parfait.feature.segmentation.impl.editor.toOutlineDistanceField
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

    // 테두리는 알맹이 실루엣에서 떨어진 거리를 재서 그린다. 원본 해상도로 재면 사진 크기만큼 무거워지므로,
    // 화면에 나올 크기로 한 번 줄여 두고 그 사본에서 잰다.
    // 줄이는 것도 원본 해상도를 훑는 일이라 알맹이를 만들 때와 마찬가지로 컴포지션 밖에서 한다
    val stamp: ToppingBorderStamp? by produceState<ToppingBorderStamp?>(initialValue = null, cutout, canvasSize) {
        val source = cutout ?: return@produceState
        val mapping = BitmapViewMapping.fitCenter(canvasSize, source.width, source.height)
        val width = (source.width * mapping.scale).roundToInt()
        val height = (source.height * mapping.scale).roundToInt()
        if (width <= 0 || height <= 0) return@produceState

        value = withContext(Dispatchers.Default) {
            val scaled = source.scale(width, height)
            ToppingBorderStamp(image = scaled.asImageBitmap(), distanceField = scaled.toOutlineDistanceField())
        }
    }

    // 거리는 그대로 두고 칠하기만 다시 하면 되므로, 굵기를 끄는 동안에도 실루엣을 다시 재지 않는다.
    // 굵기가 dp 라 화면 배율을 태우지 않는다. 사진 해상도가 달라도 눈에 보이는 굵기는 같다
    val density = LocalDensity.current.density
    val borderImage: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, stamp, borderLayers, density) {
        val current = stamp ?: return@produceState

        value = withContext(Dispatchers.Default) {
            current.distanceField
                .buildBorderBitmap(
                    targetWidth = current.image.width,
                    targetHeight = current.image.height,
                    bands = borderLayers.toBorderBands(density),
                )?.asImageBitmap()
        }
    }

    Canvas(modifier = modifier.onSizeChanged { size -> canvasSize = size }) {
        val image = stamp?.image ?: return@Canvas
        val mapping = BitmapViewMapping.fitCenter(size, originBitmap.width, originBitmap.height)

        val dstOffset = IntOffset(mapping.offsetX.roundToInt(), mapping.offsetY.roundToInt())
        val dstSize = IntSize(image.width, image.height)

        // 저장 결과가 원본 크기를 지키므로 테두리도 알맹이와 같은 크기 안에 그려져 밖으로 나간 만큼은 잘려 있다.
        // 아직 새 크기로 다시 그리기 전인 테두리는 늘어나 보이므로 크기가 맞을 때만 얹는다
        borderImage
            ?.takeIf { border -> border.width == image.width && border.height == image.height }
            ?.let { border -> drawImage(image = border, dstOffset = dstOffset, dstSize = dstSize) }

        drawImage(image = image, dstOffset = dstOffset, dstSize = dstSize)
    }
}

/** 화면 크기로 줄여 둔 알맹이와 그 실루엣에서 잰 거리. 굵기가 바뀌어도 둘 다 그대로 쓴다 */
private data class ToppingBorderStamp(
    val image: ImageBitmap,
    val distanceField: ToppingOutlineDistanceField,
)

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
