package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlin.math.roundToInt

/** ML Kit 가이드가 "at least 512x512" 를 적는다 */
internal const val DETECTION_MIN_SHORT_SIDE = 512

/** 문서 근거가 아니라 자원에서 나온 값이다. 판을 네 번 추론하므로 피크를 여기서 막는다 */
internal const val DETECTION_MAX_LONG_SIDE = 2048

/** 테스트가 구현의 오차에 맞춰지지 않게 여기서 고정한다 */
internal const val ROUND_TRIP_TOLERANCE_PX = 1

private const val FOCUS_MARGIN_RATIO = 0.20f

/** 정수로 비교한다. 부동소수 비율이면 정확히 70% 인 경계가 반올림 오차로 흔들린다 */
private const val FOCUS_SHRINK_CEILING_PERCENT = 70

private const val CENTER_CROP_RATIO = 0.70f

/**
 * 검출 공간의 사각형.
 *
 * `SegmentationBounds` 는 KDoc 이 원본 좌표를 단정하고 있어, 같은 타입으로 두 좌표계를 겸하면 짝이 안 맞는
 * 조합이 컴파일된다.
 */
internal data class DetectionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal data class ScaledSize(val width: Int, val height: Int)

/**
 * 검출 공간의 좌표를 원본 공간으로 되돌린다.
 *
 * 축마다 배율이 다른 것은 목표 치수를 정수로 반올림하기 때문이다.
 */
internal data class RecoveryTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Int,
    val offsetY: Int,
) {
    fun toOrigin(bounds: DetectionBounds): SegmentationBounds = SegmentationBounds(
        left = offsetX + (bounds.left * scaleX).roundToInt(),
        top = offsetY + (bounds.top * scaleY).roundToInt(),
        right = offsetX + (bounds.right * scaleX).roundToInt(),
        bottom = offsetY + (bounds.bottom * scaleY).roundToInt(),
    )
}

internal data class RecoveryStage(
    /** 널이면 원본 전체를 쓴다. 좌표는 원본 기준이다 */
    val cropRect: SegmentationBounds?,
    val targetSize: ScaledSize,
    val applyContrast: Boolean,
    val transform: RecoveryTransform,
)

/** 검출 공간이 원본에 어떻게 놓이는가. 1차 경로에는 없다 */
internal data class DetectionProjection(
    val transform: RecoveryTransform,
    val clip: SegmentationBounds,
)

/** 재표본은 [mapped] 크기로 하고, 그다음 [clipped] 로 자른다. 순서를 뒤집으면 알파가 어긋난다 */
internal data class ProjectedRegion(
    val mapped: SegmentationBounds,
    val clipped: SegmentationBounds,
)

/**
 * 검출에 쓸 치수. 하한과 상한이 충돌하면 상한이 이긴다 — 확대는 정보를 늘리지 않지만 상한 초과는
 * 메모리로 죽는다. 배율이 하나라 두 축은 언제나 같은 방향으로 움직인다.
 */
internal fun resolveTargetSize(
    width: Int,
    height: Int,
): ScaledSize {
    require(width > 0 && height > 0) { "size must be positive but was ${width}x$height" }

    val floorScale = maxOf(1f, DETECTION_MIN_SHORT_SIDE.toFloat() / minOf(width, height))
    val ceilingScale = DETECTION_MAX_LONG_SIDE.toFloat() / maxOf(width, height)
    val scale = minOf(floorScale, ceilingScale)

    return ScaledSize(
        width = maxOf(1, (width * scale).roundToInt()),
        height = maxOf(1, (height * scale).roundToInt()),
    )
}

/** 크롭 없는 1단계. 목표 치수가 원본과 같고 대비도 안 걸면 1차 경로의 재실행일 뿐이라 널이다 */
internal fun normalizeStage(
    width: Int,
    height: Int,
    applyContrast: Boolean,
): RecoveryStage? {
    val target = resolveTargetSize(width, height)
    if (!applyContrast && target.width == width && target.height == height) return null

    return RecoveryStage(
        cropRect = null,
        targetSize = target,
        applyContrast = applyContrast,
        transform = RecoveryTransform(
            scaleX = width.toFloat() / target.width,
            scaleY = height.toFloat() / target.height,
            offsetX = 0,
            offsetY = 0,
        ),
    )
}

/** 2단계가 자를 사각형. 힌트가 없으면 짧은 변의 70% 를 한 변으로 하는 중앙 정사각형이다 */
internal fun focusCrop(
    width: Int,
    height: Int,
    hint: DetectionBounds?,
    hintTransform: RecoveryTransform?,
): SegmentationBounds {
    if (hint == null || hintTransform == null) return centerSquare(width, height)

    val origin = hintTransform.toOrigin(hint)
    val marginX = (origin.width * FOCUS_MARGIN_RATIO).roundToInt()
    val marginY = (origin.height * FOCUS_MARGIN_RATIO).roundToInt()

    return SegmentationBounds(
        left = (origin.left - marginX).coerceIn(0, width),
        top = (origin.top - marginY).coerceIn(0, height),
        right = (origin.right + marginX).coerceIn(0, width),
        bottom = (origin.bottom + marginY).coerceIn(0, height),
    )
}

internal fun cropAreaPercent(
    crop: SegmentationBounds,
    width: Int,
    height: Int,
): Int = (crop.width.toLong() * crop.height * 100 / (width.toLong() * height)).toInt()

/** 크롭이 원본의 70% 이상이면 널이다 — 1단계 재탕에 추론만 더 쓰게 된다 */
internal fun focusStage(
    width: Int,
    height: Int,
    crop: SegmentationBounds,
    applyContrast: Boolean,
): RecoveryStage? {
    if (crop.width <= 0 || crop.height <= 0) return null

    val cropArea = crop.width.toLong() * crop.height
    if (cropArea * 100 >= width.toLong() * height * FOCUS_SHRINK_CEILING_PERCENT) return null

    val target = resolveTargetSize(crop.width, crop.height)

    return RecoveryStage(
        cropRect = crop,
        targetSize = target,
        applyContrast = applyContrast,
        transform = RecoveryTransform(
            scaleX = crop.width.toFloat() / target.width,
            scaleY = crop.height.toFloat() / target.height,
            offsetX = crop.left,
            offsetY = crop.top,
        ),
    )
}

private fun centerSquare(
    width: Int,
    height: Int,
): SegmentationBounds {
    val side = maxOf(1, (minOf(width, height) * CENTER_CROP_RATIO).roundToInt())
    val left = (width - side) / 2
    val top = (height - side) / 2

    return SegmentationBounds(
        left = left,
        top = top,
        right = left + side,
        bottom = top + side,
    )
}

/**
 * 임계를 **초과**하는 픽셀을 감싸는 사각형.
 *
 * 임계는 폴백 이진화와 같은 축에서 받는다. 더 높은 축을 쓰면 1단계가 실패한 상황에서 힌트가 구조적으로
 * 거의 항상 빈다.
 */
internal fun hintBounds(
    alpha: ByteArray,
    width: Int,
    height: Int,
    threshold: Int,
): DetectionBounds? {
    require(alpha.size == width * height) { "alpha ${alpha.size} does not match ${width}x$height" }

    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if ((alpha[row + x].toInt() and 0xFF) <= threshold) continue

            left = minOf(left, x)
            right = maxOf(right, x)
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
        }
    }

    if (right < 0) return null

    return DetectionBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
}

/**
 * 검출 사각형을 원본에 옮기고 크롭과 원본의 교집합으로 자른다. 교집합이 비면 널이다.
 *
 * 원본 경계만으로 자르면 2단계에서 크롭 밖으로 새는 사각형이 생긴다.
 */
internal fun projectRegion(
    detection: DetectionBounds,
    projection: DetectionProjection,
    width: Int,
    height: Int,
): ProjectedRegion? {
    val mapped = projection.transform.toOrigin(detection)
    if (mapped.width <= 0 || mapped.height <= 0) return null

    val clip = projection.clip
    val left = maxOf(mapped.left, clip.left, 0)
    val top = maxOf(mapped.top, clip.top, 0)
    val right = minOf(mapped.right, clip.right, width)
    val bottom = minOf(mapped.bottom, clip.bottom, height)
    if (right <= left || bottom <= top) return null

    return ProjectedRegion(
        mapped = mapped,
        clipped = SegmentationBounds(left, top, right, bottom),
    )
}

internal fun SegmentationBounds.offsetBy(
    dx: Int,
    dy: Int,
): SegmentationBounds = SegmentationBounds(
    left = left + dx,
    top = top + dy,
    right = right + dx,
    bottom = bottom + dy,
)

/**
 * 판 치수와 사각형 치수를 비교하는 검사는 사각형을 판에서 만들기 때문에 항진명제다. 실제로 깨질 수 있는
 * 것은 이쪽이고, 깨지면 예외가 아니라 `persistSubject` 의 `drawBitmap` 이 조용히 자른다.
 */
internal fun isInsideCanvas(
    bounds: SegmentationBounds,
    canvasWidth: Int,
    canvasHeight: Int,
): Boolean = bounds.left >= 0 &&
    bounds.top >= 0 &&
    bounds.right <= canvasWidth &&
    bounds.bottom <= canvasHeight
