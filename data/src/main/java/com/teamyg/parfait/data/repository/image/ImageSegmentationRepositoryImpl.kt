package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.teamyg.parfait.core.util.android.extension.decodeUriToBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.Subject
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutionException

@Singleton
class ImageSegmentationRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ImageSegmentationRepository {
    override suspend fun decodeImage(uri: String): BitmapWrapper {
        val bitmap: Bitmap = context.contentResolver.decodeUriToBitmap(uri.toUri())

        return bitmap.toAndroidBitmap()
    }

    override suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> {
        val bitmap: Bitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData() ?: return Result.failure(
            SegmentationException.ImageNotFound(null),
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val multipleSubjectOptions = SubjectSegmenterOptions
            .Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions
                    .Builder()
                    .enableSubjectBitmap()
                    .build(),
            ).build()

        val result = runSegmenter(multipleSubjectOptions, image).getOrElse { return Result.failure(it) }

        val pairs = try {
            withContext(Dispatchers.Default) {
                val job = currentCoroutineContext()[Job]
                val checkCancelled: () -> Unit = { job?.ensureActive() }
                result.toCandidatePairs(bitmap, checkCancelled)
            }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
            throw e
        } catch (e: Exception) {
            // 필터·변환이 던질 수 있는 예상 밖 실패를 화면에 토스트로 전달할 수 있게 감싼다
            return Result.failure(SegmentationException.Process(e))
        }

        if (pairs.isEmpty()) {
            repositoryLogger.i { "세그멘테이션: ML Kit 이 후보를 0건 줬다. 전경 마스크 폴백으로 내려간다" }
            return Result.success(segmentForeground(image, bitmap))
        }

        val reverted = pairs.count { it.postProcessed == null }
        if (reverted > 0) {
            // 후처리는 개선 수단이지 후보를 없앨 권한이 아니다
            repositoryLogger.i {
                "세그멘테이션 후처리: ${pairs.size}개 중 ${reverted}개를 후처리 이전 후보로 되돌린다"
            }
        }

        val candidates = try {
            withContext(Dispatchers.Default) {
                filterCandidates(pairs.map { it.postProcessed ?: it.original })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        if (candidates.isNotEmpty()) return Result.success(candidates)

        repositoryLogger.i {
            "세그멘테이션: 필터가 후보 ${pairs.size}개를 전부 걸러 냈다. 전경 마스크 폴백으로 내려간다"
        }
        return Result.success(segmentForeground(image, bitmap))
    }

    /**
     * 전경 마스크로 후보 한 개를 만든다. 다중 후보가 하나도 안 남았을 때의 폴백이다.
     *
     * ⚠️ **세그멘테이션을 한 번 더 돌린다.** 전경 마스크 옵션을 다중 후보 옵션과 함께 켜면
     * ML Kit 모듈이 `SIGSEGV` 로 죽어서(2026-08-23 실기기 확인, Galaxy A35) 두 옵션을 한
     * 요청에 실을 수 없다. 대신 이 비용은 후보가 0건인 사진에서만 든다.
     *
     * 여기서 실패하면 값으로 접는다 — 이미 1차가 성공한 흐름이고, 화면에는 "인식된 대상 없음"과
     * 같은 결과로 보이면 된다.
     */
    private suspend fun segmentForeground(
        image: InputImage,
        origin: Bitmap,
    ): List<SegmentationCandidate> {
        val options = SubjectSegmenterOptions
            .Builder()
            .enableForegroundConfidenceMask()
            .build()

        val result = runSegmenter(options, image).getOrNull() ?: return emptyList()

        return try {
            withContext(Dispatchers.Default) {
                val job = currentCoroutineContext()[Job]
                // 타입을 명시하지 않으면 `() -> Unit?` 으로 추론돼 `() -> Unit` 자리에 못 들어간다
                val checkCancelled: () -> Unit = { job?.ensureActive() }
                result.toForegroundCandidate(origin, checkCancelled)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 모델은 APK 가 아니라 Play 서비스가 내려주는 optional module 이라, 받기 전에 process 하면 실패한다.
     */
    private suspend fun runSegmenter(
        options: SubjectSegmenterOptions,
        image: InputImage,
    ): Result<SubjectSegmentationResult> {
        val segmenter = try {
            SubjectSegmentation.getClient(options)
        } catch (e: Exception) {
            return Result.failure(SegmentationException.ClientInit(e))
        }

        return try {
            segmenter.use { segmenter ->
                if (!ensureModuleInstalled(segmenter)) {
                    return Result.failure(SegmentationException.ModuleNotReady(null))
                }

                withContext(Dispatchers.IO) {
                    Result.success(Tasks.await(segmenter.process(image)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e.toSegmentationException())
        }
    }

    /** 후처리를 태울 후보 수 상한. 후처리는 `filterCandidates` 의 상한 절단 앞에 있다 */
    private val maxPostProcessCandidates = MAX_SUBJECT_COUNT + 3

    /**
     * 후처리 전후 후보를 짝지어 들고 다닌다. 후처리가 실패하거나 알파를 전멸시킨 후보를
     * **개별로** 되돌리기 위해서다 — 목록 전체가 비었을 때만 되돌리면 넷 중 하나만 전멸한 경우
     * 그 후보가 조용히 사라진다.
     */
    private class CandidatePair(
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
    private fun SubjectSegmentationResult.toCandidatePairs(
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): List<CandidatePair> {
        val floor = coverageFloorPixels(origin.width.toLong() * origin.height)

        val eligible = subjects
            .mapNotNull { subject -> subject.bitmap?.let { subject to it } }
            .filter { (_, bitmap) -> bitmap.width.toLong() * bitmap.height >= floor }
            .sortedByDescending { (_, bitmap) -> bitmap.width.toLong() * bitmap.height }

        val considered = eligible.take(maxPostProcessCandidates)
        if (eligible.size > considered.size) {
            repositoryLogger.i {
                "세그멘테이션 후처리 대상을 ${eligible.size}개 중 ${considered.size}개로 자른다"
            }
        }

        return considered.map { (subject, bitmap) ->
            buildCandidatePair(subject, bitmap, origin, checkCancelled)
        }
    }

    /**
     * ⚠️ `try` 가 픽셀 배열 할당까지 감싼다. 12MP 후보에서 `OutOfMemoryError` 가 가장 잘 나는
     * 자리가 후처리 안이 아니라 그 할당이다.
     */
    private fun buildCandidatePair(
        subject: Subject,
        bitmap: Bitmap,
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): CandidatePair {
        val postProcessed = try {
            postProcess(subject, bitmap, origin, checkCancelled)
        } catch (e: OutOfMemoryError) {
            // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
            // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
            repositoryLogger.w(e) { "세그멘테이션 후처리가 메모리로 실패해 원본 후보로 되돌린다" }
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
                sum += sumAlpha(row)
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
    private fun postProcess(
        subject: Subject,
        bitmap: Bitmap,
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): SegmentationCandidate? {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val alpha = ByteArray(width * height)
        for (index in pixels.indices) alpha[index] = (pixels[index] ushr 24).toByte()

        val result = postProcessAlpha(alpha, width, height, checkCancelled = checkCancelled) ?: return null

        repositoryLogger.i {
            "세그멘테이션 후보 부분 알파 ${result.partialAlphaPixels}/${width * height}"
        }

        val inner = result.bounds
        val unchangedWholePlate = !result.changed && inner.width == width && inner.height == height
        val trimmed = if (unchangedWholePlate) bitmap else cropWithAlpha(pixels, alpha, width, inner)

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

    /** 출력 판을 bounds 크기로 바로 만든다. 원본 크기로 만들고 나중에 자르면 큰 배열이 헛돈다 */
    private fun cropWithAlpha(
        pixels: IntArray,
        alpha: ByteArray,
        rowStride: Int,
        bounds: SegmentationBounds,
    ): Bitmap {
        val cropped = IntArray(bounds.width * bounds.height)
        for (y in 0 until bounds.height) {
            val sourceRow = (bounds.top + y) * rowStride + bounds.left
            val targetRow = y * bounds.width
            for (x in 0 until bounds.width) {
                val value = alpha[sourceRow + x].toInt() and 0xFF
                cropped[targetRow + x] =
                    if (value == 0) 0 else (value shl 24) or (pixels[sourceRow + x] and 0x00FFFFFF)
            }
        }
        return Bitmap.createBitmap(cropped, bounds.width, bounds.height, Bitmap.Config.ARGB_8888)
    }

    /**
     * 마스크가 없거나 치수가 어긋나면 빈 목록이다 — 없는 후보를 지어내지 않는다.
     */
    private fun SubjectSegmentationResult.toForegroundCandidate(
        origin: Bitmap,
        checkCancelled: () -> Unit,
    ): List<SegmentationCandidate> {
        val foregroundMask = foregroundConfidenceMask ?: return emptyList()

        val width = origin.width
        val height = origin.height

        // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
        // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
        // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
        // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
        if (foregroundMask.remaining() != width * height) return emptyList()

        val masked = try {
            maskSubjectAlpha(foregroundMask, width, height, checkCancelled = checkCancelled)
        } catch (e: OutOfMemoryError) {
            // 후처리는 개선 수단이라 실패했다고 흐름 전체를 실패로 접을 이유가 없다.
            // 기존 catch (e: Exception) 은 Error 를 안 잡으므로 여기서 따로 받는다
            repositoryLogger.w(e) { "세그멘테이션 폴백 후처리가 메모리로 실패했다" }
            null
        } ?: return emptyList()

        repositoryLogger.i {
            "세그멘테이션 폴백 부분 알파 ${masked.result.partialAlphaPixels}/${width * height}"
        }

        val bounds = masked.result.bounds

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

        return listOf(
            SegmentationCandidate(
                bounds = bounds,
                bitmap = trimmed.toAndroidBitmap(),
                canvasWidth = width,
                canvasHeight = height,
                coverageAlphaSum = masked.result.alphaSum,
            ),
        )
    }

    /**
     * 원본에서 잘라 온 [pixels] 에 후처리한 알파를 얹는다. [alpha] 는 원본 전체 좌표계라
     * [bounds] 로 오프셋을 잡아 읽는다.
     */
    private fun applyAlphaInPlace(
        pixels: IntArray,
        alpha: ByteArray,
        alphaRowStride: Int,
        bounds: SegmentationBounds,
    ) {
        for (y in 0 until bounds.height) {
            val alphaRow = (bounds.top + y) * alphaRowStride + bounds.left
            val pixelRow = y * bounds.width
            for (x in 0 until bounds.width) {
                val value = alpha[alphaRow + x].toInt() and 0xFF
                pixels[pixelRow + x] =
                    if (value == 0) 0 else (value shl 24) or (pixels[pixelRow + x] and 0x00FFFFFF)
            }
        }
    }

    override suspend fun persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult> {
        val trimmed: Bitmap = (candidate.bitmap as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        return withContext(Dispatchers.Default) {
            try {
                val trimmedFile = trimmed.saveToCacheAsPng()

                val canvasBitmap = Bitmap.createBitmap(
                    candidate.canvasWidth,
                    candidate.canvasHeight,
                    Bitmap.Config.ARGB_8888,
                )

                val subjectFile = try {
                    // 스케일하지 않고 그대로 얹는다 — ML Kit 가 준 치수와 bounds 가 어긋나더라도
                    // 그림이 찌그러지지는 않게 한다
                    Canvas(canvasBitmap).drawBitmap(
                        trimmed,
                        candidate.bounds.left.toFloat(),
                        candidate.bounds.top.toFloat(),
                        null,
                    )
                    canvasBitmap.saveToCacheAsPng()
                } finally {
                    canvasBitmap.recycle()
                }

                Result.success(
                    SegmentationResult(
                        subjectImagePath = subjectFile.absolutePath,
                        trimmedSubjectImagePath = trimmedFile.absolutePath,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
    }

    override suspend fun saveEditedImage(bitmapWrapper: BitmapWrapper): Result<String> {
        // 넘겨받은 비트맵의 수명은 넘겨준 쪽이 쥐고 있으므로 여기서 recycle 하지 않는다
        val bitmap: Bitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        return runCatching { bitmap.saveToCacheAsPng().absolutePath }
    }

    override suspend fun clearSegmentationCache() {
        withContext(Dispatchers.IO) { segmentationCacheDir.clearFiles() }
    }

    private val segmentationCacheDir: File
        get() = File(context.cacheDir, SEGMENTATION_CACHE_DIR_NAME)

    /**
     * 밀리초 이름 대신 [File.createTempFile] 을 쓰는 이유: 한 번의 세그멘테이션이 subject 와
     * trimmed 를 연달아 저장해서 같은 밀리초에 두 번 떨어질 수 있다. 그러면 뒤엣것이 앞엣것을 덮는다.
     */
    private suspend fun Bitmap.saveToCacheAsPng(): File = withContext(Dispatchers.IO) {
        val directory = segmentationCacheDir.also { it.mkdirs() }
        val file = File.createTempFile("parfait_", ".png", directory)

        file.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }

        file
    }

    /**
     * 세그멘테이션 optional module 이 준비됐는지 확인하고, 없으면 설치를 요청한 뒤 완료를 기다린다.
     *
     * 매니페스트의 `com.google.mlkit.vision.DEPENDENCIES` 는 설치 시점에 다운로드를 시작해달라는
     * 힌트일 뿐 보장이 없어서, 실제 사용 직전에 한 번 더 확인한다.
     *
     * @return 모듈을 바로 쓸 수 있으면 true
     */
    private suspend fun ensureModuleInstalled(segmenter: SubjectSegmenter): Boolean = withContext(Dispatchers.IO) {
        val moduleInstallClient = ModuleInstall.getClient(context)

        if (Tasks.await(moduleInstallClient.areModulesAvailable(segmenter)).areModulesAvailable()) {
            return@withContext true
        }

        val request = ModuleInstallRequest
            .newBuilder()
            .addApi(segmenter)
            .build()
        Tasks.await(moduleInstallClient.installModules(request))

        Tasks.await(moduleInstallClient.areModulesAvailable(segmenter)).areModulesAvailable()
    }

    /**
     * 모듈 다운로드가 끝나지 않아 실패한 경우와 그 외 처리 실패를 구분한다.
     * [Tasks.await] 는 원인을 [ExecutionException] 으로 감싸서 던지므로 한 겹 벗겨서 확인한다.
     */
    private fun Throwable.toSegmentationException(): SegmentationException {
        val cause = (this as? ExecutionException)?.cause ?: this

        return if (cause is MlKitException && cause.errorCode == MlKitException.UNAVAILABLE) {
            SegmentationException.ModuleNotReady(cause)
        } else {
            SegmentationException.Process(cause)
        }
    }
}
