// 파일명은 스펙이 정한 SegmentationRecoveryNormalizer.kt다(SegmentationInputNormalizer.kt와의 충돌 회피).
// ktlint는 파일 안 유일한 클래스 DetectionPlate 이름을 기대하므로 여기서만 끈다.
@file:Suppress("ktlint:standard:filename")

package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

/** [ownedByUs] 가 거짓이면 원본이다. 쓰지도 회수하지도 않는다 */
internal class DetectionPlate(val bitmap: Bitmap, val ownedByUs: Boolean)

/**
 * 계획을 비트맵에 적용한다. 크롭 → 축소 → 축소판에서 히스토그램 → 축소판에 LUT 순서다. 원본에서 히스토그램을
 * 모으면 그 픽셀 배열 하나가 판 하나만큼 크다.
 *
 * ⚠️ 크롭도 축소도 필요 없으면 검출 판이 곧 원본 인스턴스다. 원본은 가변으로 디코드되므로 거기에 대비를 적용하면
 * 예외 없이 사용자 사진이 바뀐다. 그 경우 먼저 복사한다.
 *
 * LUT 적용이 픽셀 루프인 것은 `minSdk` 가 26 이라 `RenderEffect` 를 못 쓰고, 임의 LUT 가
 * `ColorMatrixColorFilter` 로 표현되지 않기 때문이다.
 */
internal suspend fun normalizeForDetection(
    origin: Bitmap,
    stage: RecoveryStage,
): DetectionPlate {
    val job = currentCoroutineContext().job

    val cropped = stage.cropRect
        ?.let { rect -> Bitmap.createBitmap(origin, rect.left, rect.top, rect.width, rect.height) }
        ?: origin
    val target = stage.targetSize
    val scaled = if (cropped.width == target.width && cropped.height == target.height) {
        cropped
    } else {
        Bitmap.createScaledBitmap(cropped, target.width, target.height, true)
    }
    // 두 팩토리는 조건에 따라 입력을 그대로 돌려준다
    if (cropped !== origin && cropped !== scaled) cropped.recycle()
    job.ensureActive()

    val owned = scaled !== origin
    if (!stage.applyContrast) return DetectionPlate(scaled, owned)

    val writable = if (owned && scaled.isMutable) {
        scaled
    } else {
        val copy = requireNotNull(scaled.copy(Bitmap.Config.ARGB_8888, true)) { "detection plate copy failed" }
        if (owned) scaled.recycle()
        copy
    }
    applyContrastInPlace(writable)

    return DetectionPlate(writable, ownedByUs = true)
}

private suspend fun applyContrastInPlace(bitmap: Bitmap) {
    val job = currentCoroutineContext().job
    val width = bitmap.width
    val row = IntArray(width)
    val histogram = IntArray(LUMINANCE_LEVELS)

    for (y in 0 until bitmap.height) {
        job.ensureActive()
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (pixel in row) histogram[contrastLuminance(pixel)]++
    }

    val lut = contrastLut(histogram)

    for (y in 0 until bitmap.height) {
        job.ensureActive()
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (index in row.indices) row[index] = throughLut(row[index], lut)
        bitmap.setPixels(row, 0, width, 0, y, width, 1)
    }
}

private fun contrastLuminance(pixel: Int): Int =
    (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000

private fun throughLut(
    pixel: Int,
    lut: IntArray,
): Int = Color.argb(Color.alpha(pixel), lut[Color.red(pixel)], lut[Color.green(pixel)], lut[Color.blue(pixel)])
