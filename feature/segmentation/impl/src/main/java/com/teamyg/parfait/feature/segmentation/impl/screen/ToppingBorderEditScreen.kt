package com.teamyg.parfait.feature.segmentation.impl.screen

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
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
import com.teamyg.parfait.feature.segmentation.impl.component.toppingBorderPreviewLayoutOrNull
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingEditStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingOutlineDistanceField
import com.teamyg.parfait.feature.segmentation.impl.editor.buildCutoutBitmap
import com.teamyg.parfait.feature.segmentation.impl.editor.toBorderBands
import com.teamyg.parfait.feature.segmentation.impl.editor.toOutlineDistanceField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** 사방에 남겨 두는 여백. 가장 굵은 테두리도 다 받아낼 크기다 */
private const val MAX_BORDER_PADDING_DP = 50f

/**
 * 테두리 탭의 내용. 잘라낸 알맹이에 [borderLayers] 를 겹겹이 둘러 보여준다.
 *
 * 마스크를 직접 고치는 영역 탭과 달리 실루엣 바깥으로 색을 넓히는 작업이라 비트맵은 건드리지 않는다.
 * 알맹이는 [MAX_BORDER_PADDING_DP] 만큼 자리를 비워 둔 판 가운데에 앉는다.
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
    val density = LocalDensity.current.density
    val paddingPx = (MAX_BORDER_PADDING_DP * density).roundToInt()
    val stamp: ToppingBorderStamp? by produceState<ToppingBorderStamp?>(
        initialValue = null,
        cutout,
        canvasSize,
        paddingPx,
    ) {
        val source = cutout ?: return@produceState
        val layout = toppingBorderPreviewLayoutOrNull(
            viewWidth = canvasSize.width,
            viewHeight = canvasSize.height,
            bitmapWidth = source.width,
            bitmapHeight = source.height,
            paddingPx = paddingPx,
        ) ?: return@produceState

        value = withContext(Dispatchers.Default) {
            val scaled = source.scale(layout.subjectWidth, layout.subjectHeight)
            val padded = createBitmap(layout.canvasWidth, layout.canvasHeight).also { padded ->
                AndroidCanvas(padded).drawBitmap(scaled, paddingPx.toFloat(), paddingPx.toFloat(), null)
            }
            scaled.recycle()

            ToppingBorderStamp(
                image = padded.asImageBitmap(),
                distanceField = padded.toOutlineDistanceField(),
                offset = IntOffset(layout.offsetX, layout.offsetY),
            )
        }
    }

    // 거리는 그대로 두고 칠하기만 다시 하면 되므로, 굵기를 끄는 동안에도 실루엣을 다시 재지 않는다.
    // 굵기가 dp 라 화면 배율을 태우지 않는다. 사진 해상도가 달라도 눈에 보이는 굵기는 같다
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
        val current = stamp ?: return@Canvas
        val image = current.image

        val dstOffset = current.offset
        val dstSize = IntSize(image.width, image.height)

        // 아직 새 크기로 다시 그리기 전인 테두리는 늘어나 보이므로 크기가 맞을 때만 얹는다
        borderImage
            ?.takeIf { border -> border.width == image.width && border.height == image.height }
            ?.let { border -> drawImage(image = border, dstOffset = dstOffset, dstSize = dstSize) }

        drawImage(image = image, dstOffset = dstOffset, dstSize = dstSize)
    }
}

/**
 * 사방에 여백을 두고 화면 크기로 줄여 둔 알맹이와 그 실루엣에서 잰 거리.
 * 굵기가 바뀌어도 셋 다 그대로 쓴다.
 */
private data class ToppingBorderStamp(
    val image: ImageBitmap,
    val distanceField: ToppingOutlineDistanceField,
    val offset: IntOffset,
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
