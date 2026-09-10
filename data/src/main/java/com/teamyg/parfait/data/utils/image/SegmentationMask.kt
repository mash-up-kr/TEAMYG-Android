package com.teamyg.parfait.data.utils.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import java.nio.FloatBuffer

/** 이 신뢰도 이하는 완전히 투명하다 */
private const val RAMP_FLOOR = 0.35f

/** 이 신뢰도 이상은 완전히 불투명하다 */
private const val RAMP_CEILING = 0.65f

private const val FULLY_OPAQUE = 255

/**
 * 전경 신뢰도를 알파로 사상한다. 이진 컷 대신 램프를 쓰는 것은 경계 한두 픽셀을 부드럽게 하기
 * 위해서다.
 *
 * ⚠️ 변환은 **버림**이다. 종전 상수가 "이 값을 **넘는**" 신뢰도만 객체로 봤고, 버림이면
 * `알파 > 127 ⇔ 신뢰도 ≥ 0.35 + 128 × 0.3 / 255`(대략 0.5006)라 그 경계가 거의 그대로 옮겨진다.
 * 반올림으로 바꾸면 0.5가 전경이 되어 판정이 뒤집힌다.
 */
internal fun confidenceToAlpha(confidence: Float): Int {
    if (confidence <= RAMP_FLOOR) return 0
    if (confidence >= RAMP_CEILING) return FULLY_OPAQUE

    return (FULLY_OPAQUE * (confidence - RAMP_FLOOR) / (RAMP_CEILING - RAMP_FLOOR)).toInt()
}

internal class MaskedAlpha(
    val alpha: ByteArray,
    val result: AlphaPostProcessResult,
)

/** 신뢰도를 알파로. 검출 공간에서 돈다 */
internal fun confidenceToAlphaArray(
    mask: FloatBuffer,
    width: Int,
    height: Int,
): ByteArray {
    val alpha = ByteArray(width * height)
    for (index in alpha.indices) alpha[index] = confidenceToAlpha(mask[index]).toByte()

    return alpha
}

/**
 * 알파 후처리. 원본 공간에서 돈다 — [guidance] 가 원본을 읽기 때문이다.
 *
 * ⚠️ [alpha] 를 제자리에서 지운다. 되돌림에 쓸 알파는 부르기 전에 사본을 떠 둔다.
 */
internal suspend fun postProcessMaskedAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? {
    val result = postProcessAlpha(alpha, width, height, options, guidance = guidance) ?: return null

    return MaskedAlpha(alpha = alpha, result = result)
}

/**
 * 검출 공간과 원본 공간이 같을 때만 쓴다. 다르면 두 단계를 직접 불러 사이에 [resampleAlpha] 를 낀다.
 *
 * @param mask 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 */
internal suspend fun maskSubjectAlpha(
    mask: FloatBuffer,
    width: Int,
    height: Int,
    options: AlphaPostProcessOptions = AlphaPostProcessOptions(),
    guidance: GuidanceProvider? = null,
): MaskedAlpha? = postProcessMaskedAlpha(
    confidenceToAlphaArray(mask, width, height),
    width,
    height,
    options,
    guidance,
)

/**
 * 알파를 다른 치수로 옮긴다. 확대는 쌍선형, 축소는 박스 평균이다.
 *
 * 확대만 받게 두면 안 된다 — 짧은 변 하한이 걸리거나 크롭이 작으면 되올림이 축소가 된다. 목표 치수가 한 배율에서
 * 나오므로 두 축의 방향은 언제나 같아서 가로만 보고 가른다.
 */
internal fun resampleAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): ByteArray {
    require(alpha.size == width * height) {
        "alpha ${alpha.size} does not match ${width}x$height"
    }
    require(targetWidth > 0 && targetHeight > 0) {
        "target must be positive but was ${targetWidth}x$targetHeight"
    }

    if (targetWidth == width && targetHeight == height) return alpha.copyOf()

    return if (targetWidth < width) {
        boxAverageAlpha(alpha, width, height, targetWidth, targetHeight)
    } else {
        bilinearAlpha(alpha, width, height, targetWidth, targetHeight)
    }
}

internal fun cropAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    region: SegmentationBounds,
): ByteArray {
    require(alpha.size == width * height) {
        "alpha ${alpha.size} does not match ${width}x$height"
    }
    require(
        region.left >= 0 &&
            region.top >= 0 &&
            region.right <= width &&
            region.bottom <= height &&
            region.width > 0 &&
            region.height > 0,
    ) {
        "region $region escapes ${width}x$height"
    }

    val out = ByteArray(region.width * region.height)
    for (y in 0 until region.height) {
        System.arraycopy(alpha, (region.top + y) * width + region.left, out, y * region.width, region.width)
    }

    return out
}

/**
 * 검출 공간의 알파를 원본 좌표 사각형으로 옮긴다. [ProjectedRegion.mapped] 크기로 **먼저** 재표본하고,
 * 그다음 [ProjectedRegion.clipped] 로 자른다. 잘린 크기로 바로 재표본하면 알파가 어긋난다.
 */
internal fun projectAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    projected: ProjectedRegion,
): ByteArray {
    val mapped = projected.mapped
    val full = resampleAlpha(alpha, width, height, mapped.width, mapped.height)

    return cropAlpha(full, mapped.width, mapped.height, projected.clipped.offsetBy(-mapped.left, -mapped.top))
}

internal fun alphaSum(alpha: ByteArray): Long {
    var sum = 0L
    for (value in alpha) sum += value.toInt() and 0xFF

    return sum
}

private fun bilinearAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): ByteArray {
    val out = ByteArray(targetWidth * targetHeight)
    val scaleX = if (targetWidth > 1) (width - 1).toFloat() / (targetWidth - 1) else 0f
    val scaleY = if (targetHeight > 1) (height - 1).toFloat() / (targetHeight - 1) else 0f

    for (y in 0 until targetHeight) {
        val sourceY = y * scaleY
        val y0 = sourceY.toInt().coerceIn(0, height - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val weightY = sourceY - y0

        for (x in 0 until targetWidth) {
            val sourceX = x * scaleX
            val x0 = sourceX.toInt().coerceIn(0, width - 1)
            val x1 = (x0 + 1).coerceAtMost(width - 1)
            val weightX = sourceX - x0

            val upper = lerpAlpha(alphaAt(alpha, width, x0, y0), alphaAt(alpha, width, x1, y0), weightX)
            val lower = lerpAlpha(alphaAt(alpha, width, x0, y1), alphaAt(alpha, width, x1, y1), weightX)
            out[y * targetWidth + x] = (upper + (lower - upper) * weightY).toInt().toByte()
        }
    }

    return out
}

private fun boxAverageAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): ByteArray {
    val out = ByteArray(targetWidth * targetHeight)

    for (y in 0 until targetHeight) {
        val startY = y * height / targetHeight
        val endY = maxOf(startY + 1, (y + 1) * height / targetHeight)

        for (x in 0 until targetWidth) {
            val startX = x * width / targetWidth
            val endX = maxOf(startX + 1, (x + 1) * width / targetWidth)

            var sum = 0
            for (sourceY in startY until endY) {
                for (sourceX in startX until endX) sum += alphaAt(alpha, width, sourceX, sourceY)
            }
            out[y * targetWidth + x] = (sum / ((endY - startY) * (endX - startX))).toByte()
        }
    }

    return out
}

private fun alphaAt(
    alpha: ByteArray,
    width: Int,
    x: Int,
    y: Int,
): Int = alpha[y * width + x].toInt() and 0xFF

private fun lerpAlpha(
    from: Int,
    to: Int,
    weight: Float,
): Float = from + (to - from) * weight
