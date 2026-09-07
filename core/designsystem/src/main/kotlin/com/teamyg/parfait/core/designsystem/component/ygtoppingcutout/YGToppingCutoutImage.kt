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
import androidx.compose.ui.geometry.isSpecified
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
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 알맹이 긴 변을 이 격자로 반올림한 값이 바뀔 때만 띠를 다시 만든다 */
private const val BORDER_SIZE_QUANTUM_PX = 16

/** 판의 알맹이가 거리판의 이 배수를 넘지 않는다. 그 위로는 같은 거리판을 다시 표본화할 뿐이다 */
private const val BORDER_PLATE_FIELD_MULTIPLE = 2

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
 * @param borderWidth 화면 기준 dp 다 — 토핑 크기를 드래그해 바꾸는 동안에도 굵기는 그대로다
 */
@Composable
fun YGToppingCutoutImage(
    painter: Painter,
    borderColor: Color?,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
    outline: ToppingOutline?,
) {
    Box(modifier = modifier) {
        if (outline != null && borderColor != null && borderWidth > 0.dp) {
            ToppingBorder(outline = outline, painter = painter, color = borderColor, width = borderWidth)
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
    painter: Painter,
    color: Color,
    width: Dp,
) {
    val outsetPx = with(LocalDensity.current) { width.toPx() }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // Image 가 painter 의 intrinsic 비율로 앉으므로 띠도 같은 비율을 봐야 어긋나지 않는다.
    // 비율을 못 구할 때(비동기 painter가 아직 안 떴을 때)만 거리판 비율로 떨어진다
    val intrinsicSize = painter.intrinsicSize
    val aspectRatio = if (
        intrinsicSize.isSpecified &&
        intrinsicSize.width.isFinite() &&
        intrinsicSize.height.isFinite() &&
        intrinsicSize.height > 0f
    ) {
        intrinsicSize.width / intrinsicSize.height
    } else {
        outline.width.toFloat() / outline.height
    }

    // 실측 boxSize 는 여기서만 읽는다 — 격자([BORDER_SIZE_QUANTUM_PX])로 반올림한 "가상 표시
    // 크기"의 긴 변만 produceState 의 키로 넘긴다. 실측 그대로를 키로 두면 드래그 중 매 프레임
    // 재생성이라 격자 한 칸을 넘을 때만 다시 만들게 한다
    val quantizedSubjectLongSide = if (boxSize.width > 0 && boxSize.height > 0) {
        val subjectNow = fitSize(aspectRatio, boxSize)
        quantizeLongSide(max(subjectNow.width, subjectNow.height))
    } else {
        0
    }

    val plate: ToppingBorderPlate? by produceState<ToppingBorderPlate?>(
        initialValue = null,
        outline,
        quantizedSubjectLongSide,
        outsetPx,
        aspectRatio,
    ) {
        if (quantizedSubjectLongSide <= 0) return@produceState

        value = withContext(Dispatchers.Default) {
            // quantizedSubjectLongSide 를 "가상 표시 크기"의 긴 변으로 여기고 판을 만든다.
            // 판 해상도 상한 — 거리판을 표본화할 뿐인 지점 위로는 판을 키워도 선명해지지 않는다
            val fieldLongSide = max(outline.width, outline.height)
            val plateLongSide = min(quantizedSubjectLongSide, fieldLongSide * BORDER_PLATE_FIELD_MULTIPLE)
            val plateSubject = sizeForLongSide(aspectRatio, plateLongSide)

            // 판이 가상 표시 크기보다 작게 만들어진 만큼(상한에 걸렸을 때만 1보다 작다),
            // 판 좌표계에서 쓰는 굵기·여백도 같은 비율로 줄인다 — 안 줄이면 늘려 그릴 때 두꺼워진다
            val plateScale = plateLongSide.toFloat() / quantizedSubjectLongSide
            val plateOutsetPx = outsetPx * plateScale
            val platePadding = ceil(plateOutsetPx).toInt() + 1

            val target = ToppingBorderTarget(
                width = plateSubject.width + platePadding * 2,
                height = plateSubject.height + platePadding * 2,
                subjectLeft = platePadding,
                subjectTop = platePadding,
                subjectWidth = plateSubject.width,
                subjectHeight = plateSubject.height,
            )

            outline.toBorderAlphaBitmap(target, plateOutsetPx)?.asImageBitmap()?.let { image ->
                ToppingBorderPlate(image = image, padding = platePadding)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .matchParentSize()
            .onSizeChanged { size -> boxSize = size },
    ) {
        val current = plate ?: return@Canvas
        val currentBoxWidth = size.width.roundToInt()
        val currentBoxHeight = size.height.roundToInt()

        // 판은 양자화·해상도 상한 때문에 지금 상자보다 작게(가상 크기로) 만들어졌을 수 있다.
        // 자리와 크기를 둘 다 지금 상자 기준 실측으로 다시 재, 판을 그 실제 알맹이 크기로 늘려 그린다
        val realSubject = fitSize(aspectRatio, IntSize(currentBoxWidth, currentBoxHeight))
        val plateSubjectWidth = current.image.width - current.padding * 2
        val drawScale = if (plateSubjectWidth > 0) realSubject.width.toFloat() / plateSubjectWidth else 1f
        val scaledPadding = (current.padding * drawScale).roundToInt()

        drawImage(
            image = current.image,
            dstOffset = IntOffset(
                x = (currentBoxWidth - realSubject.width) / 2 - scaledPadding,
                y = (currentBoxHeight - realSubject.height) / 2 - scaledPadding,
            ),
            dstSize = IntSize(
                width = (current.image.width * drawScale).roundToInt(),
                height = (current.image.height * drawScale).roundToInt(),
            ),
            colorFilter = ColorFilter.tint(color),
        )
    }
}

/**
 * 알맹이와 여백을 함께 담은 띠 한 장. 양자화·해상도 상한을 적용한 가상 크기로 만들어져 실제
 * 상자보다 작을 수 있다 — [padding]을 포함한 판 전체를, 그릴 때 실제 알맹이 크기에 맞춰 늘린다
 */
private data class ToppingBorderPlate(
    val image: ImageBitmap,
    val padding: Int,
)

private fun fitSize(
    aspectRatio: Float,
    box: IntSize,
): IntSize = if (box.width / aspectRatio <= box.height) {
    IntSize(box.width, (box.width / aspectRatio).roundToInt())
} else {
    IntSize((box.height * aspectRatio).roundToInt(), box.height)
}

/** 알맹이 긴 변을 [BORDER_SIZE_QUANTUM_PX] 격자로 반올림한다. 항상 격자 한 칸 이상을 돌려준다 */
private fun quantizeLongSide(longSide: Int): Int {
    val rounded = (longSide + BORDER_SIZE_QUANTUM_PX / 2) / BORDER_SIZE_QUANTUM_PX * BORDER_SIZE_QUANTUM_PX
    return rounded.coerceAtLeast(BORDER_SIZE_QUANTUM_PX)
}

/** [aspectRatio] 를 지키며 긴 변이 [longSide] 인 크기를 만든다 — [fitSize] 의 역방향이다 */
private fun sizeForLongSide(
    aspectRatio: Float,
    longSide: Int,
): IntSize = if (aspectRatio >= 1f) {
    IntSize(longSide, (longSide / aspectRatio).roundToInt())
} else {
    IntSize((longSide * aspectRatio).roundToInt(), longSide)
}

@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    // 프리뷰 전용 합성 실루엣 — 그림(R.drawable.ic_plus)과 종횡비를 맞출 필요는 없다, 목적은 테두리를
    // 눈에 보이게 하는 것이다
    val previewOutline = remember {
        val side = 40
        ToppingOutline.of(side, side) { x, y ->
            val dx = x - side / 2f
            val dy = y - side / 2f
            if (dx * dx + dy * dy <= (side / 2f) * (side / 2f)) 255 else 0
        }
    }

    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
        outline = previewOutline,
    )
}
