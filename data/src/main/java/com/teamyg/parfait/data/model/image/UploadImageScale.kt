package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import kotlin.math.roundToInt

/**
 * 긴 변 상한. iOS 와 맞춘 값이라 한쪽만 바꾸면 같은 캔버스가 기기별로 다른 화질이 된다
 * (근거는 `specs/2026-09-08-upload-image-downscale.md` 「결정 표」).
 */
private const val NUKKI_LONG_SIDE_LIMIT = 1500
private const val BACKGROUND_LONG_SIDE_LIMIT = 2048

/** PNG 는 무손실이라 이 값을 보지 않는다 */
const val UPLOAD_JPEG_QUALITY = 90

data class UploadImageSize(
    val width: Int,
    val height: Int,
)

sealed interface UploadImagePlan {
    data object Passthrough : UploadImagePlan

    /** @param sampleSize 디코드 단계에서 미리 줄일 배수 */
    data class Reencode(
        val targetSize: UploadImageSize,
        val sampleSize: Int,
        val format: UploadImageFormat,
    ) : UploadImagePlan
}

/**
 * 치수와 포맷 둘 다 그대로여도 되는지 판정한다. 어느 한쪽이라도 바뀌어야 다시 굽는다 —
 * 이미 JPEG 이고 상한 이하인 배경을 다시 구우면 손실만 더해진다.
 */
fun planUploadImage(
    sourceSize: UploadImageSize,
    imageType: ImageType,
    sourceFormat: UploadImageFormat,
): UploadImagePlan {
    val targetSize = scaledSize(sourceSize, longSideLimitOf(imageType))
    val targetFormat = uploadFormatOf(imageType, sourceFormat)

    if (targetSize == sourceSize && targetFormat == sourceFormat) return UploadImagePlan.Passthrough

    return UploadImagePlan.Reencode(
        targetSize = targetSize,
        sampleSize = sampleSizeOf(sourceSize, targetSize),
        format = targetFormat,
    )
}

private fun longSideLimitOf(imageType: ImageType): Int = when (imageType) {
    ImageType.NUKKI -> NUKKI_LONG_SIDE_LIMIT
    ImageType.BACKGROUND -> BACKGROUND_LONG_SIDE_LIMIT
}

/** 배경은 캔버스를 덮는 불투명 이미지라 알파를 버려도 잃는 것이 없다. 누끼는 입력을 따라간다 */
private fun uploadFormatOf(
    imageType: ImageType,
    sourceFormat: UploadImageFormat,
): UploadImageFormat = when (imageType) {
    ImageType.NUKKI -> sourceFormat
    ImageType.BACKGROUND -> UploadImageFormat.JPEG
}

/** 상한 이하면 손대지 않는다 — 확대는 정보를 늘리지 않으면서 바이트만 키운다 */
private fun scaledSize(
    sourceSize: UploadImageSize,
    longSideLimit: Int,
): UploadImageSize {
    val longSide = maxOf(sourceSize.width, sourceSize.height)
    if (longSide <= longSideLimit) return sourceSize

    val ratio = longSideLimit.toDouble() / longSide
    return UploadImageSize(
        width = (sourceSize.width * ratio).roundToInt().coerceAtLeast(1),
        height = (sourceSize.height * ratio).roundToInt().coerceAtLeast(1),
    )
}

/**
 * 목표보다 작아지지 않는 선까지만 2 의 거듭제곱으로 줄인다. 넘겨서 줄이면 뒤에서 확대하게 되고,
 * `BitmapFactory` 는 2 의 거듭제곱이 아닌 값을 그 아래 거듭제곱으로 내림한다.
 */
private fun sampleSizeOf(
    sourceSize: UploadImageSize,
    targetSize: UploadImageSize,
): Int {
    var sampleSize = 1
    while (sourceSize.width / (sampleSize * 2) >= targetSize.width &&
        sourceSize.height / (sampleSize * 2) >= targetSize.height
    ) {
        sampleSize *= 2
    }
    return sampleSize
}
