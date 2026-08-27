package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

private const val OPAQUE = 255

/**
 * [keep] 이 거짓인 자리의 알파를 0으로 만든다. 참인 자리는 **원본 알파를 그대로 둔다.**
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal suspend fun applyKeepMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    keep: BooleanArray,
    maskWidth: Int,
    factor: Int,
): Boolean {
    val job = currentCoroutineContext().job
    var changed = false

    for (y in 0 until height) {
        job.ensureActive()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            val index = rowOffset + x
            if ((alpha[index].toInt() and 0xFF) == 0) continue
            if (keep[maskRowOffset + x / factor]) continue

            alpha[index] = 0
            changed = true
        }
    }

    return changed
}

internal data class AlphaMeasurement(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    /** 알파가 1~254 인 픽셀 수. 관측이 램프 띠 폭과 침식 유효성을 판정하는 데 쓴다 */
    val partialAlphaPixels: Int,
)

/**
 * 남은 알파를 감싸는 사각 영역과 커버리지를 잰다.
 *
 * 불투명 판정을 "알파 0 초과"로 두는 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「tight bounds 판정 기준 통일」에 있다.
 *
 * @return 남은 알파가 없으면 `null`
 */
internal suspend fun measureAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
): AlphaMeasurement? {
    val job = currentCoroutineContext().job
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    var sum = 0L
    var partial = 0

    for (y in 0 until height) {
        job.ensureActive()
        val rowOffset = y * width
        for (x in 0 until width) {
            val value = alpha[rowOffset + x].toInt() and 0xFF
            if (value == 0) continue

            sum += value
            if (value < OPAQUE) partial++
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    if (right < 0) return null

    return AlphaMeasurement(
        // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
        bounds = SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1),
        alphaSum = sum,
        partialAlphaPixels = partial,
    )
}

private const val ABSENT = -1

/**
 * 경계를 한 겹 안으로 깎는다. `a' = min(a, 4-근방 알파의 최소)` 다.
 *
 * ⚠️ **판정은 침식 전 값으로 한다.** 제자리에서 래스터 순서로 돌리면 방금 낮아진 왼쪽 값을 다음
 * 픽셀이 읽어, 부분 알파 띠가 스캔 방향으로만 연쇄 침식된다. 반대 방향은 한 겹만 깎이므로 피사체가
 * 한쪽으로 밀린 것처럼 보인다. 그래서 직전 행의 침식 전 알파 한 줄과 좌측 픽셀의 침식 전 값을
 * 들고 돈다. 오른쪽·아래는 아직 안 고쳤으므로 현재 배열을 그대로 읽는다.
 *
 * **능선 보호**: 마주 보는 4-근방 쌍(좌·우 또는 상·하)이 둘 다 0이면 건너뛴다.
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * 이미지 밖 이웃은 최소 계산에서는 빠진다(`ABSENT` 가 `-1` 이라 `in 0 until lowest` 에 걸리지 않는다).
 * 반면 능선 판정에서는 0이 아닌 값으로 세어져 "이웃이 있다"고 본다 — 밖을 능선 판정에서도 빼면 한
 * 행짜리 판에서 상·하 쌍이 공집합이라 공허참으로 보호되어 하드 매트가 아무 일도 안 하게 된다.
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal suspend fun erodeEdge(
    alpha: ByteArray,
    width: Int,
    height: Int,
): Boolean {
    val job = currentCoroutineContext().job
    if (width <= 0 || height <= 0) return false

    var previousRow = ByteArray(width)
    var currentRow = ByteArray(width)
    var changed = false

    for (y in 0 until height) {
        job.ensureActive()
        val rowOffset = y * width
        alpha.copyInto(currentRow, 0, rowOffset, rowOffset + width)

        var leftBefore = ABSENT
        for (x in 0 until width) {
            val here = currentRow[x].toInt() and 0xFF
            val left = leftBefore
            val right = if (x < width - 1) currentRow[x + 1].toInt() and 0xFF else ABSENT
            val up = if (y > 0) previousRow[x].toInt() and 0xFF else ABSENT
            val down = if (y < height - 1) alpha[rowOffset + width + x].toInt() and 0xFF else ABSENT
            leftBefore = here

            if (here == 0) continue
            if (left == 0 && right == 0) continue
            if (up == 0 && down == 0) continue

            var lowest = here
            if (left in 0 until lowest) lowest = left
            if (right in 0 until lowest) lowest = right
            if (up in 0 until lowest) lowest = up
            if (down in 0 until lowest) lowest = down

            if (lowest != here) {
                alpha[rowOffset + x] = lowest.toByte()
                changed = true
            }
        }

        val spare = previousRow
        previousRow = currentRow
        currentRow = spare
    }

    return changed
}

/** 원본 픽셀 환산. 이 값 **미만** 크기의 성분은 잡티로 보고 버린다 */
internal const val AREA_OPENING_MIN_PIXELS = 256

/** 판정 버퍼를 줄이지 않는 크기 하한. 이 값 **미만** 픽셀 수의 판은 배율 1로 돈다 */
internal const val MIN_PIXELS_FOR_DOWNSCALE = 2_000_000

/** 정련 계수를 구할 배율. 판정 버퍼 배율(`downscaleFactor`)과 **별개 값**이다 */
internal const val REFINE_DOWNSCALE = 4

/** 축소판 기준 창 반경. 값의 근거는 `synthesis/open-questions.md` OQ-P-298 */
internal const val REFINE_RADIUS = 2

/** 정칙화. 작을수록 안내자를 바싹 따라간다. 근거는 OQ-P-298 */
internal const val REFINE_EPSILON = 1e-4f

internal data class AlphaPostProcessOptions(
    val downscaleFactor: Int = 4,
    val binaryThreshold: Int = 127,
    val areaOpeningMinPixels: Int = AREA_OPENING_MIN_PIXELS,
    val erodeEdge: Boolean = true,
    val minPixelsForDownscale: Int = MIN_PIXELS_FOR_DOWNSCALE,
    val refineEdges: Boolean = true,
    val refineDownscale: Int = REFINE_DOWNSCALE,
    val refineRadius: Int = REFINE_RADIUS,
    val refineEpsilon: Float = REFINE_EPSILON,
) {
    init {
        // 배율은 판정 좌표를 나누는 데 네 자리에서 쓰인다. 만들어지는 자리에서 막아야
        // 어느 자리가 터지든 원인이 이 값이라는 것이 드러난다
        require(downscaleFactor >= 1) { "downscaleFactor must be >= 1 but was $downscaleFactor" }
    }
}

internal data class AlphaPostProcessResult(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    val partialAlphaPixels: Int,
    /**
     * 거짓이면 알파가 하나도 안 바뀌었다는 뜻이다. 원본 판을 그대로 쓰려면 [bounds] 가 판 전체와
     * 같은지도 함께 봐야 한다 — 알파를 안 바꿔도 원판에 투명 여백이 있으면 판 치수와 [bounds]
     * 치수가 어긋나 `SegmentationCandidate` 의 계약이 깨진다.
     */
    val changed: Boolean,
    /** 정련에 든 시간. 안 돌았으면 0 이다. 원본 해상도 적용이 감당 가능한지 판정할 근거다 */
    val refineElapsedNanos: Long,
)

/**
 * 정련이 쓸 안내자를 공급한다. 커널이 `Bitmap` 을 모른다는 원칙을 지키면서 두 경로가 서로 다른
 * 방식으로 픽셀을 대게 하는 통로다.
 */
internal fun interface GuidanceProvider {
    /** [bounds] 크기의 ARGB. 행 우선이고 stride 는 `bounds.width` 다 */
    fun pixelsIn(bounds: SegmentationBounds): IntArray
}

/**
 * [alpha] 를 그 자리에서 다듬고 남은 영역을 돌려준다.
 *
 * 판정(이진화·성분·팽창)은 축소판에서, 적용과 측정은 원본 해상도에서 한다. 축소판이 정하는 것은
 * "이 영역이 살아남는 성분인가"뿐이고 경계 모양은 원본 알파가 그대로 만든다. 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「처리 해상도」.
 *
 * @param alpha 길이가 `width * height` 여야 한다
 * @return 남은 알파가 없으면 `null`. 정련이나 침식 단계에서 전멸했다면 `alpha` 는 이미 지워진 채로
 *   `null` 이 나간다 — `applyAreaOpening` 이 전멸을 보고하는 경로는 `alpha` 를 원본 그대로 두고
 *   반환하므로 다르다
 */
internal suspend fun postProcessAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): AlphaPostProcessResult? {
    require(alpha.size == width * height) {
        "alpha length ${alpha.size} does not match ${width}x$height"
    }
    if (width <= 0 || height <= 0) return null

    val factor = if (width.toLong() * height < options.minPixelsForDownscale) 1 else options.downscaleFactor
    val maskWidth = ceilDiv(width, factor)
    val maskHeight = ceilDiv(height, factor)

    val mask = downscaleMask(alpha, width, height, factor, options.binaryThreshold)

    val minComponentPixels = maxOf(1, options.areaOpeningMinPixels / (factor * factor))
    if (!applyAreaOpening(mask, maskWidth, maskHeight, minComponentPixels)) return null

    val keep = dilateMask(mask, maskWidth, maskHeight)

    val applied = applyKeepMask(alpha, width, height, keep, maskWidth, factor)

    // 정련이 훑을 영역을 먼저 정한다. 창 통계를 내는 연산이라 빈 여백까지 훑으면 값 없이 비싸다
    val beforeRefine = if (options.refineEdges && guidance != null) {
        measureAlpha(alpha, width, height) ?: return null
    } else {
        null
    }

    val startedAt = System.nanoTime()
    val refined = beforeRefine != null &&
        guidance != null &&
        refineWithin(alpha, width, beforeRefine.bounds, guidance, options)
    val refineElapsedNanos = if (beforeRefine != null) System.nanoTime() - startedAt else 0L

    val eroded = options.erodeEdge && erodeEdge(alpha, width, height)

    val measured = if (beforeRefine != null && !refined && !eroded) {
        beforeRefine
    } else {
        measureAlpha(alpha, width, height) ?: return null
    }

    return AlphaPostProcessResult(
        bounds = measured.bounds,
        alphaSum = measured.alphaSum,
        partialAlphaPixels = measured.partialAlphaPixels,
        changed = applied || refined || eroded,
        refineElapsedNanos = refineElapsedNanos,
    )
}

/**
 * [bounds] 사각형만 잘라 정련하고 되쓴다. 잘라 내는 이유는 [refineAlpha] 가 연속된 배열을
 * 전제하기 때문이고, 그 전제를 유지하는 편이 stride 를 함수 넷에 실어 나르는 것보다 싸다.
 */
private suspend fun refineWithin(
    alpha: ByteArray,
    rowStride: Int,
    bounds: SegmentationBounds,
    guidance: GuidanceProvider,
    options: AlphaPostProcessOptions,
): Boolean {
    val patch = ByteArray(bounds.width * bounds.height)
    for (y in 0 until bounds.height) {
        val source = (bounds.top + y) * rowStride + bounds.left
        alpha.copyInto(patch, y * bounds.width, source, source + bounds.width)
    }

    val changed = refineAlpha(
        alpha = patch,
        guidance = guidance.pixelsIn(bounds),
        width = bounds.width,
        height = bounds.height,
        downscale = options.refineDownscale,
        radius = options.refineRadius,
        epsilon = options.refineEpsilon,
    )
    if (!changed) return false

    for (y in 0 until bounds.height) {
        val target = (bounds.top + y) * rowStride + bounds.left
        patch.copyInto(alpha, target, y * bounds.width, y * bounds.width + bounds.width)
    }

    return true
}
