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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.segmentation.impl.editor.ToppingBorderStroke
import com.teamyg.parfait.feature.segmentation.impl.editor.buildEditedBitmap
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditState
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 실루엣을 부풀릴 때 원 둘레를 몇 번 찍을지.
 *
 * 알파 마스크를 실제로 팽창시키는 대신 같은 이미지를 반지름만큼 떨어진 자리에 빙 둘러 찍고,
 * 겹친 자국으로 굵은 윤곽을 만든다. 수가 적으면 윤곽에 각이 지고, 많을수록 그리기가 무거워진다.
 */
private const val OUTLINE_SAMPLE_COUNT = 24

private const val FULL_TURN_RADIANS = 2 * Math.PI

/**
 * 테두리 탭의 내용. 잘라낸 결과에 [ToppingEditState.borderStrokes] 를 겹겹이 둘러 보여준다.
 *
 * 마스크를 직접 고치는 영역 탭과 달리 결과 이미지 바깥으로 색을 넓히는 작업이라,
 * 비트맵은 그대로 두고 그릴 때만 실루엣을 부풀려 얹는다.
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

        // 겹이 안쪽부터 쌓여 있으므로 그릴 때는 가장 바깥부터 깔아야 안쪽 겹이 위에 남는다
        state.borderStrokes.withOutsets().asReversed().forEach { (stroke, outset) ->
            drawOutline(
                image = editedImage,
                dstOffset = dstOffset,
                dstSize = dstSize,
                radius = outset.dp.toPx(),
                color = stroke.color,
            )
        }

        drawImage(image = editedImage, dstOffset = dstOffset, dstSize = dstSize)
    }
}

/**
 * 각 겹과 그 겹이 실루엣에서 밀려나야 할 거리를 짝지어 돌려준다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로, 밀려나는 거리는 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 */
private fun List<ToppingBorderStroke>.withOutsets(): List<Pair<ToppingBorderStroke, Float>> {
    var accumulated = 0f
    return map { stroke ->
        accumulated += stroke.width
        stroke to accumulated
    }
}

/**
 * [image] 의 실루엣을 [radius] 만큼 부풀려 [color] 로 칠한다.
 *
 * 알파는 두고 색만 갈아끼우는 tint 라 반투명한 가장자리도 원래 모양대로 번진다.
 */
private fun DrawScope.drawOutline(
    image: ImageBitmap,
    dstOffset: IntOffset,
    dstSize: IntSize,
    radius: Float,
    color: Color,
) {
    // 투명한 겹은 칠할 것이 없다. 그래도 자기 굵기만큼 자리는 차지해 바깥 겹을 더 밀어낸다
    if (radius <= 0f || color.alpha == 0f) return

    val colorFilter = ColorFilter.tint(color)
    repeat(OUTLINE_SAMPLE_COUNT) { index ->
        val angle = FULL_TURN_RADIANS * index / OUTLINE_SAMPLE_COUNT
        drawImage(
            image = image,
            dstOffset = IntOffset(
                x = dstOffset.x + (cos(angle) * radius).roundToInt(),
                y = dstOffset.y + (sin(angle) * radius).roundToInt(),
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
