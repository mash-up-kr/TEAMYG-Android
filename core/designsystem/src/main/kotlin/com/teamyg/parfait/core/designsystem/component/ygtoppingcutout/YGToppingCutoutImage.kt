package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.outline.toBorderAlphaBitmap
import com.teamyg.parfait.core.util.jvm.outline.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 누끼 외곽선을 찍는 방향 수. 터치 판정이 같은 방향으로 되민 점을 읽으므로 이 값이 정본이다.
 *
 * ⚠️ 그리는 쪽은 더 이상 이 값을 쓰지 않는다. 판정이 거리판으로 옮겨 가면 함께 사라진다.
 */
const val TOPPING_OUTLINE_STAMP_COUNT = 8

/**
 * 크기가 연달아 바뀌는 동안에는 띠를 만들지 않고 멎기를 기다린다.
 *
 * 다음 크기 변화가 이 대기를 취소하므로 핀치 한 번에 띠를 한 벌만 만든다. 그래도 되는 이유는
 * 핀치로 크기가 변하는 화면에서 움직이는 토핑이 하나이기 때문이다.
 */
private const val BORDER_REBUILD_DELAY_MS = 48L

/**
 * 누끼 이미지와 그 실루엣을 따르는 테두리를 함께 그린다. 사각 테두리를 두르면 잘라 낸 배경이 다시
 * 드러나므로, 실루엣에서 잰 거리로 띠를 만들어 알맹이 아래에 깔고 그 위에 원본을 얹는다.
 *
 * 테두리를 그리는 화면이 여럿이라 여기서 한 벌만 둔다(`adr/0030-topping-outline-distance-field.md`).
 *
 * ⚠️ **띠는 이 컴포저블의 상자 밖으로 [borderWidth] 만큼 나간다.** 부르는 쪽이 클리핑 레이어나
 * `alpha < 1` 을 씌우면 그만큼 잘리므로, 그런 자리는 상자를 굵기만큼 키우고 안쪽으로 덜어내야 한다.
 *
 * @param outline 준비되기 전에는 `null` 이다 — 그동안은 테두리 없이 알맹이만 그린다
 * @param borderWidth 화면 기준 dp 다 — 토핑을 키워도 굵기는 그대로다
 */
@Composable
fun YGToppingCutoutImage(
    painter: Painter,
    borderColor: Color?,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
    outline: ToppingOutline? = null,
) {
    Box(modifier = modifier) {
        if (outline != null && borderColor != null && borderWidth > 0.dp) {
            ToppingBorder(outline = outline, color = borderColor, width = borderWidth)
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BoxScope.ToppingBorder(
    outline: ToppingOutline,
    color: Color,
    width: Dp,
) {
    val outsetPx = with(LocalDensity.current) { width.toPx() }
    val padding = ceil(outsetPx).toInt() + 1

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val plate: ToppingBorderPlate? by produceState<ToppingBorderPlate?>(
        initialValue = null,
        outline,
        boxSize,
        outsetPx,
    ) {
        val box = boxSize
        if (box.width <= 0 || box.height <= 0) return@produceState

        delay(BORDER_REBUILD_DELAY_MS)

        value = withContext(Dispatchers.Default) {
            // 알맹이는 Fit 으로 앉으므로 상자가 아니라 실루엣 비율로 그려질 자리를 구한다
            val subject = fitSize(outline.width.toFloat() / outline.height, box)
            val target = ToppingBorderTarget(
                width = subject.width + padding * 2,
                height = subject.height + padding * 2,
                subjectLeft = padding,
                subjectTop = padding,
                subjectWidth = subject.width,
                subjectHeight = subject.height,
            )

            outline.toBorderAlphaBitmap(target, outsetPx)?.asImageBitmap()?.let { image ->
                ToppingBorderPlate(
                    image = image,
                    offset = IntOffset(
                        x = (box.width - subject.width) / 2 - padding,
                        y = (box.height - subject.height) / 2 - padding,
                    ),
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .onSizeChanged { size -> boxSize = size },
    ) {
        val current = plate ?: return@Canvas
        drawImage(
            image = current.image,
            dstOffset = current.offset,
            dstSize = IntSize(current.image.width, current.image.height),
            colorFilter = ColorFilter.tint(color),
        )
    }
}

/** 알맹이와 여백을 함께 담은 띠 한 장과 그것을 놓을 자리 */
private data class ToppingBorderPlate(
    val image: ImageBitmap,
    val offset: IntOffset,
)

private fun fitSize(
    aspectRatio: Float,
    box: IntSize,
): IntSize = if (box.width / aspectRatio <= box.height) {
    IntSize(box.width, (box.width / aspectRatio).roundToInt())
} else {
    IntSize((box.height * aspectRatio).roundToInt(), box.height)
}

@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
    )
}
