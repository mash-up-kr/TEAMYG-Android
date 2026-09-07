package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    // Image 가 painter 의 intrinsic 비율로 앉으므로 띠도 같은 비율을 봐야 어긋나지 않는다
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

    // 굵기·비율을 키로 두면 안 된다 — 그 둘이 바뀌는 순간에는 캐시가 반드시 미스라, 상태를 비우면
    // 새 판이 올 때까지 테두리가 사라진다. 옛 판을 두고 아래 이펙트가 갈아 끼운다
    var plate by remember(outline) {
        mutableStateOf(cachedToppingBorderPlate(outline, outsetPx, aspectRatio))
    }

    // 크기를 키로 두면 바뀔 때마다 만들던 판을 버리고 다시 시작해, 판 한 장이 한 프레임보다 오래
    // 걸리는 드래그 중에는 어느 판도 끝을 못 본다. conflate 로 받아 한 번에 한 장씩 끝까지 만든다
    LaunchedEffect(outline, outsetPx, aspectRatio) {
        cachedToppingBorderPlate(outline, outsetPx, aspectRatio)?.let { cached -> plate = cached }

        snapshotFlow { boxSize }
            .conflate()
            .collect { size ->
                // 돌아온 뒤에 넣으면 그사이 취소됐을 때 다 만든 판이 버려진다
                val built = withContext(Dispatchers.Default) {
                    buildBorderPlate(outline, aspectRatio, outsetPx, size) { isActive }
                        ?.also { made -> cacheToppingBorderPlate(outline, outsetPx, aspectRatio, made) }
                }
                if (built != null) plate = built
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

        // 판은 지금 상자와 크기가 다를 수 있어, 자리와 크기를 지금 상자 기준으로 다시 잰다
        val realSubject = fitSize(aspectRatio, IntSize(currentBoxWidth, currentBoxHeight))

        // 너무 어긋난 판을 늘려 그리면 굵기 dp 고정이 깨진다 — 새 판이 올 때까지 안 그린다
        if (!current.fitsSubject(max(realSubject.width, realSubject.height))) return@Canvas

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
 * [boxSize] 안에 앉을 알맹이에 맞는 띠 한 장을 만든다. 그리는 스레드 밖에서 부른다.
 *
 * @param shouldContinue `false` 를 답하면 훑던 판을 버리고 `null` 을 돌려준다
 */
private fun buildBorderPlate(
    outline: ToppingOutline,
    aspectRatio: Float,
    outsetPx: Float,
    boxSize: IntSize,
    shouldContinue: () -> Boolean,
): ToppingBorderPlate? {
    val subject = fitSize(aspectRatio, boxSize)
    val subjectLongSide = max(subject.width, subject.height)
    if (subjectLongSide <= 0) return null

    // 판 해상도 상한 — 거리판을 표본화할 뿐인 지점 위로는 판을 키워도 선명해지지 않는다
    val fieldLongSide = max(outline.width, outline.height)
    val plateLongSide = min(subjectLongSide, fieldLongSide * BORDER_PLATE_FIELD_MULTIPLE)
    val plateSubject = sizeForLongSide(aspectRatio, plateLongSide)

    // 판이 알맹이보다 작아진 만큼 굵기·여백도 줄인다 — 안 줄이면 늘려 그릴 때 두꺼워진다
    val plateScale = plateLongSide.toFloat() / subjectLongSide
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

    val bitmap = outline.toBorderAlphaBitmap(target, plateOutsetPx, shouldContinue) ?: return null

    return ToppingBorderPlate(
        image = bitmap.asImageBitmap(),
        padding = platePadding,
        subjectLongSide = subjectLongSide,
    )
}

private fun fitSize(
    aspectRatio: Float,
    box: IntSize,
): IntSize = if (box.width / aspectRatio <= box.height) {
    IntSize(box.width, (box.width / aspectRatio).roundToInt())
} else {
    IntSize((box.height * aspectRatio).roundToInt(), box.height)
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
