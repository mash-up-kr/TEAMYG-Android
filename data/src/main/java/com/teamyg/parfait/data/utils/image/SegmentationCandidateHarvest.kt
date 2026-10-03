package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import com.google.mlkit.vision.segmentation.subject.Subject
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
import com.teamyg.parfait.data.model.image.HarvestedCandidate
import com.teamyg.parfait.data.model.image.MlKitPlate
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SubjectCoverage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import java.nio.FloatBuffer

/** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
internal const val MAX_POST_PROCESS_CANDIDATES = MAX_SUBJECT_COUNT + 3

/**
 * bbox 사전 절단은 원본 좌표 사각형 면적으로 한다. bbox 픽셀 수는 커버리지의 상계라 하한 미만이면 커버리지도
 * 하한 미만이다.
 */
internal suspend fun harvestSubjects(
    subjects: List<Subject>,
    origin: Bitmap,
): List<HarvestedCandidate> {
    val job = currentCoroutineContext().job
    val floor = SubjectCoverage.floorPixels(origin.width.toLong() * origin.height)

    val withPlate = subjects.mapNotNull { subject -> subject.bitmap?.let { subject to it } }
    val placed = withPlate.map { (subject, plate) ->
        // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고 보장하지 않으므로 판에서 뽑는다
        val region = SegmentationBounds(
            left = subject.startX,
            top = subject.startY,
            right = subject.startX + plate.width,
            bottom = subject.startY + plate.height,
        )
        MlKitPlate(plate, region)
    }

    val eligible = placed
        .filter { it.region.area() >= floor }
        .sortedByDescending { it.region.area() }

    repositoryLogger.i {
        "세그멘테이션 후보 쌍 생성: subject ${subjects.size}개 중 판 있음 ${withPlate.size}개, " +
            "bbox 하한 통과 ${eligible.size}개"
    }

    val considered = eligible.take(MAX_POST_PROCESS_CANDIDATES)
    if (eligible.size > considered.size) {
        repositoryLogger.i { "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다" }
    }

    return considered.mapNotNull { plate ->
        job.ensureActive()
        harvestCandidate(plate, origin)
    }
}

private fun SegmentationBounds.area(): Long = width.toLong() * height

/** 캔버스 밖으로 새는 후보는 흐름 전체가 아니라 그 후보만 버린다 */
private suspend fun harvestCandidate(
    source: MlKitPlate,
    origin: Bitmap,
): HarvestedCandidate? {
    val region = source.region
    if (!isInsideCanvas(region, origin.width, origin.height)) {
        repositoryLogger.w { "세그멘테이션 후보 $region 이 캔버스 ${origin.width}x${origin.height} 밖이라 버린다" }
        return null
    }

    return harvestMlKitPlate(source, origin)
}

private fun originGuidance(
    origin: Bitmap,
    region: SegmentationBounds,
) = GuidanceProvider { bounds ->
    IntArray(bounds.width * bounds.height).also { pixels ->
        origin.getPixels(
            pixels,
            0,
            bounds.width,
            region.left + bounds.left,
            region.top + bounds.top,
            bounds.width,
            bounds.height,
        )
    }
}

/**
 * `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는 자리가 후처리 안이
 * 아니라 그 할당이다.
 */
private suspend fun harvestMlKitPlate(
    source: MlKitPlate,
    origin: Bitmap,
): HarvestedCandidate {
    val postProcessed = try {
        postProcessMlKitPlate(source.plate, source.region, origin)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 후처리가 메모리로 실패해 원본 후보로 되돌린다" }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        repositoryLogger.w(e) { "세그멘테이션 후처리가 예외로 실패해 원본 후보로 되돌린다" }
        null
    }

    if (postProcessed != null) return HarvestedCandidate(postProcessed, reverted = false)

    return HarvestedCandidate(mlKitOriginal(source.plate, source.region, origin), reverted = true)
}

/** ML Kit 판은 후처리가 지우지 않았으므로 되돌림 커버리지를 그 판에서 센다 */
private fun mlKitOriginal(
    plate: Bitmap,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate {
    require(plate.width == region.width && plate.height == region.height) {
        "plate ${plate.width}x${plate.height} does not match region $region"
    }

    // 행 단위로 읽는다 — 판 전체 크기 버퍼는 후처리가 메모리로 실패한 직후 같은 크기를 한 번 더 요구한다
    val row = IntArray(plate.width)
    var coverage = 0L
    for (y in 0 until plate.height) {
        plate.getPixels(row, 0, plate.width, 0, y, plate.width, 1)
        coverage += row.sumArgbAlpha()
    }

    return SegmentationCandidate(
        bounds = region,
        bitmap = plate.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = coverage,
    )
}

private suspend fun postProcessMlKitPlate(
    plate: Bitmap,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate? {
    val width = plate.width
    val height = plate.height
    val pixels = IntArray(width * height)
    plate.getPixels(pixels, 0, width, 0, 0, width, height)

    val alpha = ByteArray(width * height)
    for (index in pixels.indices) alpha[index] = (pixels[index] ushr 24).toByte()

    val result = postProcessAlpha(alpha, width, height, guidance = originGuidance(origin, region)) ?: run {
        repositoryLogger.i { "세그멘테이션 후처리가 후보 ${width}x$height 판의 알파를 전부 지워 원본으로 되돌린다" }
        return null
    }

    repositoryLogger.i {
        "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}, " +
            "정련 ${result.refineElapsedNanos / 1_000_000}ms"
    }

    val inner = result.bounds
    // ML Kit 판은 ML Kit 소유라 알파가 안 바뀌면 그대로 써도 된다(OQ-P-266)
    val unchangedWholePlate = !result.changed && inner.width == width && inner.height == height
    val trimmed = if (unchangedWholePlate) {
        plate
    } else {
        Bitmap.createBitmap(
            composeCroppedArgb(pixels, alpha, width, inner),
            inner.width,
            inner.height,
            Bitmap.Config.ARGB_8888,
        )
    }

    return SegmentationCandidate(
        bounds = inner.offsetBy(region.left, region.top),
        bitmap = trimmed.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = result.alphaSum,
    )
}

/** 마스크는 원본 좌표 원점에 놓인다고 본다 — 크기만 검사하고 위치는 옮기지 않는다 */
internal suspend fun harvestForeground(
    mask: FloatBuffer,
    maskWidth: Int,
    maskHeight: Int,
    origin: Bitmap,
): List<SegmentationCandidate> {
    // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로 remaining() 으로 비교한다
    if (mask.remaining() != maskWidth * maskHeight) {
        repositoryLogger.w {
            "세그멘테이션 폴백 마스크 길이 불일치: 남은 ${mask.remaining()}, 기대 ${maskWidth}x$maskHeight"
        }
        return emptyList()
    }

    // 마스크 크기 할당이라 이것도 OOM 가드 안에 둔다
    val detectionAlpha = try {
        confidenceToAlphaArray(mask, maskWidth, maskHeight)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 신뢰도 되올림이 메모리로 실패했다" }
        return emptyList()
    }

    val region = SegmentationBounds(0, 0, maskWidth, maskHeight)
    if (!isInsideCanvas(region, origin.width, origin.height)) return emptyList()

    val masked = try {
        postProcessMaskedAlpha(detectionAlpha, region.width, region.height, guidance = originGuidance(origin, region))
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
        null
    } ?: return emptyList()

    repositoryLogger.i {
        "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${region.width * region.height}, " +
            "정련 ${masked.result.refineElapsedNanos / 1_000_000}ms"
    }

    val local = masked.result.bounds
    val bounds = local.offsetBy(region.left, region.top)

    // 이 판이 폴백에서 가장 큰 할당이라 OOM 가드를 여기까지 넓힌다
    val candidate = try {
        val trimmedPixels = IntArray(local.width * local.height)
        origin.getPixels(trimmedPixels, 0, local.width, bounds.left, bounds.top, local.width, local.height)
        applyAlphaInPlace(trimmedPixels, masked.alpha, region.width, local)

        SegmentationCandidate(
            bounds = bounds,
            bitmap = Bitmap
                .createBitmap(trimmedPixels, local.width, local.height, Bitmap.Config.ARGB_8888)
                .toAndroidBitmap(),
            canvasWidth = origin.width,
            canvasHeight = origin.height,
            coverageAlphaSum = masked.result.alphaSum,
        )
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 판 생성이 메모리로 실패했다" }
        null
    }

    return listOfNotNull(candidate)
}
