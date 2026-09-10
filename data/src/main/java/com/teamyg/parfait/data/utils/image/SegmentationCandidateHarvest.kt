package com.teamyg.parfait.data.utils.image

import android.graphics.Bitmap
import com.google.mlkit.vision.segmentation.subject.Subject
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SubjectCoverage
import kotlinx.coroutines.CancellationException

/** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
internal const val MAX_POST_PROCESS_CANDIDATES = MAX_SUBJECT_COUNT + 3

/**
 * 후처리 전후 후보를 짝지어 들고 다닌다. 후처리가 실패하거나 알파를 전멸시킨 후보를
 * **개별로** 되돌리기 위해서다 — 목록 전체가 비었을 때만 되돌리면 넷 중 하나만 전멸한 경우
 * 그 후보가 조용히 사라진다.
 */
internal class CandidatePair(
    val original: SegmentationCandidate,
    val postProcessed: SegmentationCandidate?,
)

/**
 * `getBitmap()` 은 널을 돌려줄 수 있다 — `enableSubjectBitmap()` 을 켰다는 이유로 비널을
 * 단정하지 않는다. 판이 없는 후보는 고를 수 없으므로 버린다.
 *
 * 후처리 전에 bbox 로 값싸게 자르는 이유: bbox 픽셀 수는 커버리지의 상계라, 하한 미만이면
 * 커버리지도 하한 미만이다. 최종 판정을 바꾸지 않으면서 큰 판을 훑는 일을 건너뛴다.
 */
internal suspend fun SubjectSegmentationResult.toCandidatePairs(origin: Bitmap): List<CandidatePair> {
    val floor = SubjectCoverage.floorPixels(origin.width.toLong() * origin.height)

    val withBitmap = subjects.mapNotNull { subject -> subject.bitmap?.let { subject to it } }
    val eligible = withBitmap
        .filter { (_, bitmap) -> bitmap.width.toLong() * bitmap.height >= floor }
        .sortedByDescending { (_, bitmap) -> bitmap.width.toLong() * bitmap.height }

    // 0건 원인을 가르는 데 쓴다 — subject 자체가 0건인지, bitmap 이 널이라 빠졌는지,
    // bbox 사전 절단에서 하한 미만으로 빠졌는지가 로그 한 줄로 갈린다
    repositoryLogger.i {
        "세그멘테이션 후보 쌍 생성: subject ${subjects.size}개 중 판 있음 ${withBitmap.size}개, " +
            "bbox 하한 통과 ${eligible.size}개"
    }

    val considered = eligible.take(MAX_POST_PROCESS_CANDIDATES)
    if (eligible.size > considered.size) {
        repositoryLogger.i {
            "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다"
        }
    }

    return considered.map { (subject, bitmap) ->
        buildCandidatePair(subject, bitmap, origin)
    }
}

/**
 * ⚠️ `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는
 * 자리가 후처리 안이 아니라 그 할당이다.
 */
private suspend fun buildCandidatePair(
    subject: Subject,
    bitmap: Bitmap,
    origin: Bitmap,
): CandidatePair {
    val postProcessed = try {
        postProcess(subject, bitmap, origin)
    } catch (e: OutOfMemoryError) {
        // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
        // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
        repositoryLogger.w(e) { "세그멘테이션 후처리가 메모리로 실패해 원본 후보로 되돌린다" }
        null
    } catch (e: CancellationException) {
        // CancellationException 은 Exception 을 상속하므로 아래 catch 보다 먼저 잡아 다시 던진다
        throw e
    } catch (e: Exception) {
        // subject 와 origin 의 치수 불일치 등으로 getPixels 가 던질 수 있다 — 이 후보만
        // 되돌리고 세그멘테이션 전체를 실패로 접지 않는다
        repositoryLogger.w(e) { "세그멘테이션 후처리가 예외로 실패해 원본 후보로 되돌린다" }
        null
    }

    return CandidatePair(
        // 후처리가 성공하면 이 후보는 안 쓰이므로 커버리지 계산을 건너뛴다
        original = originalCandidate(subject, bitmap, origin, countCoverage = postProcessed == null),
        postProcessed = postProcessed,
    )
}

/** 되돌리는 후보는 후처리 이전 알파로 커버리지를 채운다. 커널 결과가 없으므로 직접 센다 */
private fun originalCandidate(
    subject: Subject,
    bitmap: Bitmap,
    origin: Bitmap,
    countCoverage: Boolean,
): SegmentationCandidate {
    val coverage = if (countCoverage) {
        // 행 단위로 읽는다 — 후보 판 전체 크기 버퍼를 잡으면 후처리가 메모리로 실패한 직후에
        // 같은 크기를 한 번 더 요구하게 된다
        val row = IntArray(bitmap.width)
        var sum = 0L
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            sum += row.sumArgbAlpha()
        }
        sum
    } else {
        0L
    }

    val bounds = SegmentationBounds(
        // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다.
        // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고
        // 보장하지 않으므로, subject 가 아니라 bitmap 에서 치수를 뽑는다
        left = subject.startX,
        top = subject.startY,
        right = subject.startX + bitmap.width,
        bottom = subject.startY + bitmap.height,
    )
    require(bitmap.width == bounds.width && bitmap.height == bounds.height) {
        "bitmap ${bitmap.width}x${bitmap.height} does not match bounds ${bounds.width}x${bounds.height}"
    }

    return SegmentationCandidate(
        bounds = bounds,
        bitmap = bitmap.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = coverage,
    )
}

/**
 * ⚠️ **자르기는 알파를 바꾸지 않는다.** 후처리 결과를 픽셀에 반영하려면 새 판을 만들어야 한다.
 * ML Kit 판에 되쓰는 것은 안 된다 — 그 판의 수명은 `SubjectSegmentationResult` 가 쥐고 있고
 * 네이티브에서 온 비트맵이 immutable 이면 예외다. 소유권 논의는
 * `synthesis/open-questions.md` 의 OQ-P-266 에 있다.
 */
private suspend fun postProcess(
    subject: Subject,
    bitmap: Bitmap,
    origin: Bitmap,
): SegmentationCandidate? {
    val width = bitmap.width
    val height = bitmap.height

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val alpha = ByteArray(width * height)
    for (index in pixels.indices) alpha[index] = (pixels[index] ushr 24).toByte()

    val result = postProcessAlpha(
        alpha,
        width,
        height,
        guidance = { bounds ->
            IntArray(bounds.width * bounds.height).also { guidancePixels ->
                origin.getPixels(
                    guidancePixels,
                    0,
                    bounds.width,
                    subject.startX + bounds.left,
                    subject.startY + bounds.top,
                    bounds.width,
                    bounds.height,
                )
            }
        },
    ) ?: run {
        // 후처리 이전 알파는 있었는데(비었으면 애초에 후보가 안 됐다) 커널이 전부 지웠다는
        // 뜻이다 — OOM 되돌림과 달리 임계 튜닝 신호로 값이 있다
        repositoryLogger.i {
            "세그멘테이션 후처리가 후보 ${width}x$height 판의 알파를 전부 지워 원본으로 되돌린다"
        }
        return null
    }

    repositoryLogger.i {
        "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}, " +
            "정련 ${result.refineElapsedNanos / 1_000_000}ms"
    }

    val inner = result.bounds
    val unchangedWholePlate = !result.changed && inner.width == width && inner.height == height
    val trimmed = if (unchangedWholePlate) {
        bitmap
    } else {
        val cropped = composeCroppedArgb(pixels, alpha, width, inner)
        Bitmap.createBitmap(cropped, inner.width, inner.height, Bitmap.Config.ARGB_8888)
    }

    require(trimmed.width == inner.width && trimmed.height == inner.height) {
        "trimmed ${trimmed.width}x${trimmed.height} does not match bounds ${inner.width}x${inner.height}"
    }

    return SegmentationCandidate(
        bounds = SegmentationBounds(
            left = subject.startX + inner.left,
            top = subject.startY + inner.top,
            right = subject.startX + inner.right,
            bottom = subject.startY + inner.bottom,
        ),
        bitmap = trimmed.toAndroidBitmap(),
        canvasWidth = origin.width,
        canvasHeight = origin.height,
        coverageAlphaSum = result.alphaSum,
    )
}

/**
 * 마스크가 없거나 치수가 어긋나면 빈 목록이다 — 없는 후보를 지어내지 않는다.
 */
internal suspend fun SubjectSegmentationResult.toForegroundCandidate(origin: Bitmap): List<SegmentationCandidate> {
    val foregroundMask = foregroundConfidenceMask ?: return emptyList()

    val width = origin.width
    val height = origin.height

    // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
    // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
    // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
    // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
    if (foregroundMask.remaining() != width * height) return emptyList()

    val masked = try {
        maskSubjectAlpha(
            foregroundMask,
            width,
            height,
            guidance = { bounds ->
                IntArray(bounds.width * bounds.height).also { guidancePixels ->
                    origin.getPixels(
                        guidancePixels,
                        0,
                        bounds.width,
                        bounds.left,
                        bounds.top,
                        bounds.width,
                        bounds.height,
                    )
                }
            },
        )
    } catch (e: OutOfMemoryError) {
        // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
        // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
        repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
        null
    } ?: return emptyList()

    repositoryLogger.i {
        "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${width * height}, " +
            "정련 ${masked.result.refineElapsedNanos / 1_000_000}ms"
    }

    val bounds = masked.result.bounds

    // ⚠️ 이 판이 이 폴백에서 가장 큰 두 할당이다 — bounds 크기 IntArray 와 그걸 감싸는
    // 네이티브 비트맵. 위 maskSubjectAlpha 의 ByteArray(w*h) 보다 훨씬 커서(폴백은 항상
    // 원본 해상도), OOM 가드를 여기까지 넓힌다. buildCandidatePair 의 KDoc 이 같은 이유를
    // 적어 뒀다 — try 가 픽셀 배열 할당까지 감싸지 않으면 가장 위험한 자리가 가드 밖에 남는다
    val candidate = try {
        // 살아남은 영역만 읽는다. 원본 크기 픽셀 배열과 원본 크기 중간 판을 만들었다가 자르면
        // 12MP 사진에서 그 둘만 100MB 가까이 든다
        val trimmedPixels = IntArray(bounds.width * bounds.height)
        origin.getPixels(
            trimmedPixels,
            0,
            bounds.width,
            bounds.left,
            bounds.top,
            bounds.width,
            bounds.height,
        )
        applyAlphaInPlace(trimmedPixels, masked.alpha, width, bounds)

        val trimmed = Bitmap.createBitmap(
            trimmedPixels,
            bounds.width,
            bounds.height,
            Bitmap.Config.ARGB_8888,
        )
        require(trimmed.width == bounds.width && trimmed.height == bounds.height) {
            "trimmed ${trimmed.width}x${trimmed.height} does not match bounds ${bounds.width}x${bounds.height}"
        }

        SegmentationCandidate(
            bounds = bounds,
            bitmap = trimmed.toAndroidBitmap(),
            canvasWidth = width,
            canvasHeight = height,
            coverageAlphaSum = masked.result.alphaSum,
        )
    } catch (e: OutOfMemoryError) {
        repositoryLogger.w(e) { "세그멘테이션 폴백 판 생성이 메모리로 실패했다" }
        null
    } ?: return emptyList()

    return listOf(candidate)
}
