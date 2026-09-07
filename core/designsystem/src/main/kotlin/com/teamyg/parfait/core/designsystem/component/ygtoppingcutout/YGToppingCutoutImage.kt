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

    // 컴포지션 밖에 남은 판이 있으면 그것부터 그린다 — 화면 전환·Spotlight 로 이 컴포저블이 다시
    // 만들어질 때마다 판을 처음부터 만들면 그동안 테두리가 없어 깜빡인다.
    // 굵기·비율은 키가 아니다. 그 둘이 바뀌면 아직 만든 판이 없어 캐시가 미스이고, 상태를 비우면
    // 새 판이 올 때까지 테두리가 사라진다. 옛 판을 그대로 두고 아래 이펙트가 갈아 끼운다
    var plate by remember(outline) {
        mutableStateOf(cachedToppingBorderPlate(outline, outsetPx, aspectRatio))
    }

    // 크기를 이펙트의 키로 두면 크기가 바뀔 때마다 만들던 판을 취소하고 처음부터 다시 시작한다.
    // 판 한 장을 만드는 데 한 프레임보다 오래 걸리면 드래그하는 내내 어느 판도 끝을 못 봐서 띠가
    // 멈춘 채로 남는다. 그래서 크기는 키가 아니라 conflate 한 흐름으로 받아, 판은 언제나 한 번에
    // 한 장씩 끝까지 만들고 그동안 지나간 중간 크기는 버린다 — 드래그가 멎으면 마지막 크기 한
    // 장만 남아 실측 크기로 수렴한다
    LaunchedEffect(outline, outsetPx, aspectRatio) {
        // 굵기·비율이 바뀌었을 때 그 조합으로 만들어 둔 판이 있으면 기다리지 않고 바로 쓴다
        cachedToppingBorderPlate(outline, outsetPx, aspectRatio)?.let { cached -> plate = cached }

        snapshotFlow { boxSize }
            .conflate()
            .collect { size ->
                // 캐시에 넣는 것도 만든 자리에서 한다. 돌아온 뒤에 넣으면 그사이 취소됐을 때
                // 다 만든 판이 버려진다
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

        // 판은 해상도 상한이나 만드는 사이에 바뀐 크기 때문에 지금 상자와 다른 크기일 수 있다.
        // 자리와 크기를 둘 다 지금 상자 기준 실측으로 다시 재, 판을 그 실제 알맹이 크기로 늘려 그린다
        val realSubject = fitSize(aspectRatio, IntSize(currentBoxWidth, currentBoxHeight))

        // 굵기가 판에 구워져 있어 늘려 그리면 화면상 굵기도 같은 배율로 늘어난다. 너무 어긋난
        // 판을 그리면 굵기 dp 고정 계약이 깨지므로, 그럴 때는 새 판이 올 때까지 안 그린다
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

    // 판이 알맹이보다 작게 만들어진 만큼(상한에 걸렸을 때만 1보다 작다), 판 좌표계에서 쓰는
    // 굵기·여백도 같은 비율로 줄인다 — 안 줄이면 늘려 그릴 때 두꺼워진다
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
