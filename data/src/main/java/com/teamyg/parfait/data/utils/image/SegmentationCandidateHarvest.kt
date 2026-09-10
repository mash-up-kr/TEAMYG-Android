package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import com.google.mlkit.vision.segmentation.subject.Subject
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
import com.teamyg.parfait.data.model.image.DetectionBounds
import com.teamyg.parfait.data.model.image.DetectionProjection
import com.teamyg.parfait.data.model.image.ProjectedRegion
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
 * 로그에만 쓰는 완화 배수. **판정에는 쓰지 않는다** — 수동 편집이 같은 엄격 하한으로 저장을 막으므로, 회복 판정만
 * 완화하면 고른 후보를 손대지 않고도 저장하지 못한다. 근거는 스펙 「제외」.
 */
internal const val RELAXED_FLOOR_LOG_DIVISOR = 4

/** 판 한 장이 어디서 오는가. 어느 쪽을 쓸지는 [harvestSubjects] 가 투영 유무로 정한다 */
internal sealed interface PlateSource {
    /** 1차 경로 전용. 검출 공간이 곧 원본 공간이라 이 판의 픽셀이 원본 색이다 */
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource

    /**
     * 회복 경로 전용. **픽셀 인자가 없다** — 알파만 [detectionPlate] 에서 가져오고 픽셀은 원본에서 읽는다.
     * 검출 판은 대비를 건 판이라 그 픽셀을 쓰면 결과 색이 변한다.
     */
    class OriginRegion(val detectionPlate: Bitmap, val projected: ProjectedRegion) : PlateSource
}

internal class HarvestedCandidate(
    val candidate: SegmentationCandidate,
    /** 후처리가 실패하거나 알파를 전멸시켜 후처리 이전 판으로 되돌렸다 */
    val reverted: Boolean,
)

internal class ForegroundHarvest(
    val candidates: List<SegmentationCandidate>,
    /** 다음 단계가 어디를 크롭할지 정하는 데만 쓴다. 검출 공간 좌표다 */
    val hint: DetectionBounds?,
)

private class PlacedSubject(val plate: Bitmap, val projected: ProjectedRegion)

/**
 * subject 들을 후보로 만든다. 1차 경로와 회복 경로가 함께 쓴다.
 *
 * [projection] 이 없으면 1차다. **판의 출처를 호출부가 고르지 않는 것이 요점이다** — 회복 경로가 ML Kit 판을
 * 쓸 길이 없다.
 *
 * bbox 사전 절단은 원본 좌표 사각형 면적으로 한다. bbox 픽셀 수는 커버리지의 상계라 하한 미만이면 커버리지도
 * 하한 미만이다.
 */
internal suspend fun harvestSubjects(
    subjects: List<Subject>,
    origin: Bitmap,
    projection: DetectionProjection?,
): List<HarvestedCandidate> {
    val job = currentCoroutineContext().job
    val floor = SubjectCoverage.floorPixels(origin.width.toLong() * origin.height)

    val withPlate = subjects.mapNotNull { subject -> subject.bitmap?.let { subject to it } }
    val placed = withPlate.mapNotNull { (subject, plate) ->
        // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고 보장하지 않으므로 판에서 뽑는다
        val detection = DetectionBounds(
            left = subject.startX,
            top = subject.startY,
            right = subject.startX + plate.width,
            bottom = subject.startY + plate.height,
        )
        val projected = if (projection == null) {
            val region = SegmentationBounds(detection.left, detection.top, detection.right, detection.bottom)
            ProjectedRegion(mapped = region, clipped = region)
        } else {
            projectRegion(detection, projection, origin.width, origin.height) ?: return@mapNotNull null
        }
        PlacedSubject(plate, projected)
    }

    val eligible = placed
        .filter { it.projected.clipped.area() >= floor }
        .sortedByDescending { it.projected.clipped.area() }

    repositoryLogger.i {
        // 1차 로그 한 줄은 그대로 둔다. 회복 경로에서만 완화했다면 통과했을 수를 덧붙인다
        val relaxed = if (projection == null) {
            ""
        } else {
            val count = placed.count { it.projected.clipped.area() >= floor / RELAXED_FLOOR_LOG_DIVISOR }
            "(1/$RELAXED_FLOOR_LOG_DIVISOR 하한이었다면 ${count}개)"
        }
        "세그멘테이션 후보 쌍 생성: subject ${subjects.size}개 중 판 있음 ${withPlate.size}개, " +
            "bbox 하한 통과 ${eligible.size}개$relaxed"
    }

    val considered = eligible.take(MAX_POST_PROCESS_CANDIDATES)
    if (eligible.size > considered.size) {
        repositoryLogger.i { "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다" }
    }

    return considered.mapNotNull { subject ->
        job.ensureActive()
        val source = if (projection == null) {
            PlateSource.MlKitPlate(subject.plate, subject.projected.clipped)
        } else {
            PlateSource.OriginRegion(subject.plate, subject.projected)
        }
        harvestCandidate(source, origin)
    }
}

private fun SegmentationBounds.area(): Long = width.toLong() * height

/** 캔버스 밖으로 새는 후보는 흐름 전체가 아니라 그 후보만 버린다 */
private suspend fun harvestCandidate(
    source: PlateSource,
    origin: Bitmap,
): HarvestedCandidate? {
    val region = when (source) {
        is PlateSource.MlKitPlate -> source.region
        is PlateSource.OriginRegion -> source.projected.clipped
    }
    if (!isInsideCanvas(region, origin.width, origin.height)) {
        repositoryLogger.w { "세그멘테이션 후보 $region 이 캔버스 ${origin.width}x${origin.height} 밖이라 버린다" }
        return null
    }

    return when (source) {
        is PlateSource.MlKitPlate -> harvestMlKitPlate(source, origin)
        is PlateSource.OriginRegion -> harvestOriginRegion(source, origin)
    }
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
 * ⚠️ `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는 자리가 후처리 안이
 * 아니라 그 할당이다.
 */
private suspend fun harvestMlKitPlate(
    source: PlateSource.MlKitPlate,
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
    // ML Kit 판은 ML Kit 소유라 알파가 안 바뀌면 그대로 써도 된다(OQ-P-266). 회복 경로는 이 최적화를 안 쓴다
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

/**
 * 회복 경로의 후보. **판을 항상 새로 만든다** — 재사용할 판이 원본뿐이다.
 *
 * ⚠️ `postProcessAlpha` 는 알파를 제자리에서 지운다. 되돌림에 쓸 알파는 그 전에 사본을 떠 둔다. 원본 픽셀의
 * 알파는 JPEG 에서 전부 255 라, 사본 없이 되돌리면 불투명 사각형이 커버리지 만점으로 필터 1위에 오른다.
 *
 * 후보 하나의 실패는 그 후보만 버린다.
 */
private suspend fun harvestOriginRegion(
    source: PlateSource.OriginRegion,
    origin: Bitmap,
): HarvestedCandidate? {
    val region = source.projected.clipped

    return try {
        val alpha = originAlphaOf(source)
        val pristine = alpha.copyOf()
        val pixels = IntArray(region.width * region.height)
        origin.getPixels(pixels, 0, region.width, region.left, region.top, region.width, region.height)

        val postProcessed = try {
            postProcessOriginRegion(pixels, alpha, region, origin)
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 후처리가 메모리로 실패해 사본 알파로 되돌린다" }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 후처리가 예외로 실패해 사본 알파로 되돌린다" }
            null
        }

        if (postProcessed != null) {
            HarvestedCandidate(postProcessed, reverted = false)
        } else {
            HarvestedCandidate(originRegionReverted(pixels, pristine, region, origin), reverted = true)
        }
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "회복 후보 $region 이 메모리로 실패해 버린다" }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        repositoryLogger.w(e) { "회복 후보 $region 이 예외로 실패해 버린다" }
        null
    }
}

private fun originAlphaOf(source: PlateSource.OriginRegion): ByteArray {
    val plate = source.detectionPlate
    val row = IntArray(plate.width)
    val detectionAlpha = ByteArray(plate.width * plate.height)
    for (y in 0 until plate.height) {
        plate.getPixels(row, 0, plate.width, 0, y, plate.width, 1)
        for (x in 0 until plate.width) detectionAlpha[y * plate.width + x] = (row[x] ushr 24).toByte()
    }

    return projectAlpha(detectionAlpha, plate.width, plate.height, source.projected)
}

private suspend fun postProcessOriginRegion(
    pixels: IntArray,
    alpha: ByteArray,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate? {
    val result = postProcessAlpha(alpha, region.width, region.height, guidance = originGuidance(origin, region))
        ?: return null
    val inner = result.bounds
    // composeCroppedArgb 는 알파 채널을 덮어쓴다. 원본 픽셀의 255 가 남지 않는다
    val cropped = composeCroppedArgb(pixels, alpha, region.width, inner)

    return SegmentationCandidate(
        bounds = inner.offsetBy(region.left, region.top),
        bitmap = Bitmap.createBitmap(cropped, inner.width, inner.height, Bitmap.Config.ARGB_8888).toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = result.alphaSum,
    )
}

private fun originRegionReverted(
    pixels: IntArray,
    pristine: ByteArray,
    region: SegmentationBounds,
    origin: Bitmap,
): SegmentationCandidate {
    val whole = SegmentationBounds(0, 0, region.width, region.height)
    val plate = composeCroppedArgb(pixels, pristine, region.width, whole)

    return SegmentationCandidate(
        bounds = region,
        bitmap = Bitmap.createBitmap(plate, region.width, region.height, Bitmap.Config.ARGB_8888).toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = alphaSum(pristine),
    )
}

/**
 * 전경 신뢰도에서 후보 하나와 힌트를 만든다.
 *
 * ⚠️ 마스크 치수와 출력 치수가 다를 수 있다 — 회복 경로의 마스크는 검출 판 치수다. 하나로 묶으면 길이 검사가
 * 언제나 실패해 예외도 로그도 없이 빈 목록이 되고, 연쇄로 2단계 힌트까지 사라진다.
 *
 * @param hintThreshold 널이면 힌트를 구하지 않는다. 1차 경로에 원본 전체를 한 번 더 훑는 비용을 얹지 않는다
 */
internal suspend fun harvestForeground(
    mask: FloatBuffer,
    maskWidth: Int,
    maskHeight: Int,
    origin: Bitmap,
    projection: DetectionProjection?,
    hintThreshold: Int?,
): ForegroundHarvest {
    // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로 remaining() 으로 비교한다
    if (mask.remaining() != maskWidth * maskHeight) {
        repositoryLogger.w {
            "세그멘테이션 폴백 마스크 길이 불일치: 남은 ${mask.remaining()}, 기대 ${maskWidth}x$maskHeight"
        }
        return ForegroundHarvest(emptyList(), hint = null)
    }

    // ⚠️ 이 할당은 OOM 가드 안에 있어야 한다. OutOfMemoryError 는 Exception 이 아니라 Error 라
    // 일반 Exception 캐치로는 안 잡힌다
    val detectionAlpha = try {
        confidenceToAlphaArray(mask, maskWidth, maskHeight)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 신뢰도 되올림이 메모리로 실패했다" }
        return ForegroundHarvest(emptyList(), hint = null)
    }
    val hint = hintThreshold?.let { hintBounds(detectionAlpha, maskWidth, maskHeight, it) }

    val placed = try {
        placeForegroundAlpha(detectionAlpha, maskWidth, maskHeight, origin, projection)
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 알파 되올림이 메모리로 실패했다" }
        null
    } ?: return ForegroundHarvest(emptyList(), hint)

    val (region, alpha) = placed
    if (!isInsideCanvas(region, origin.width, origin.height)) return ForegroundHarvest(emptyList(), hint)

    val masked = try {
        postProcessMaskedAlpha(alpha, region.width, region.height, guidance = originGuidance(origin, region))
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
        null
    } ?: return ForegroundHarvest(emptyList(), hint)

    repositoryLogger.i {
        "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${region.width * region.height}, " +
            "정련 ${masked.result.refineElapsedNanos / 1_000_000}ms"
    }

    val local = masked.result.bounds
    // 로컬 사각형에 원점을 더한다. 빠뜨리면 1단계는 오프셋이 0 이라 멀쩡하고 2단계만 엉뚱한 곳을 오린다
    val bounds = local.offsetBy(region.left, region.top)

    // ⚠️ 이 판이 폴백에서 가장 큰 할당이라 OOM 가드를 여기까지 넓힌다
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

    return ForegroundHarvest(listOfNotNull(candidate), hint)
}

/** 1차는 마스크가 곧 원본 전체다. 회복은 사상 사각형 크기로 재표본한 뒤 잘린 사각형으로 자른다 */
private fun placeForegroundAlpha(
    detectionAlpha: ByteArray,
    maskWidth: Int,
    maskHeight: Int,
    origin: Bitmap,
    projection: DetectionProjection?,
): Pair<SegmentationBounds, ByteArray>? {
    if (projection == null) return SegmentationBounds(0, 0, maskWidth, maskHeight) to detectionAlpha

    val projected = projectRegion(DetectionBounds(0, 0, maskWidth, maskHeight), projection, origin.width, origin.height)
        ?: return null

    return projected.clipped to projectAlpha(detectionAlpha, maskWidth, maskHeight, projected)
}
