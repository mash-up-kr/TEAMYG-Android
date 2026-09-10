package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.SystemClock
import com.teamyg.parfait.core.util.android.extension.decodeUriToBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.data.source.image.remote.RemoteImageDownloadDataSource
import com.teamyg.parfait.domain.model.SegmentationCandidate
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.data.installer.image.ModuleInstallOutcome
import com.teamyg.parfait.data.installer.image.SegmentationModuleInstaller
import com.teamyg.parfait.data.utils.image.AlphaPostProcessOptions
import com.teamyg.parfait.data.utils.image.DetectionBounds
import com.teamyg.parfait.data.utils.image.DetectionProjection
import com.teamyg.parfait.data.utils.image.RELAXED_FLOOR_LOG_DIVISOR
import com.teamyg.parfait.data.utils.image.RecoveryStage
import com.teamyg.parfait.data.utils.image.RecoveryTransform
import com.teamyg.parfait.data.utils.image.SEGMENTATION_CACHE_DIR_NAME
import com.teamyg.parfait.data.utils.image.clearFiles
import com.teamyg.parfait.data.utils.image.cropAreaPercent
import com.teamyg.parfait.data.utils.image.filterCandidates
import com.teamyg.parfait.data.utils.image.focusCrop
import com.teamyg.parfait.data.utils.image.focusStage
import com.teamyg.parfait.data.utils.image.harvestForeground
import com.teamyg.parfait.data.utils.image.harvestSubjects
import com.teamyg.parfait.data.utils.image.normalizeForDetection
import com.teamyg.parfait.data.utils.image.normalizeStage
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.SegmentationBounds
import com.teamyg.parfait.domain.model.SubjectCoverage
import com.teamyg.parfait.domain.model.image.SourceLongSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutionException

@Singleton
class ImageSegmentationRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val remoteImageDownloadDataSource: RemoteImageDownloadDataSource,
    private val moduleInstaller: SegmentationModuleInstaller,
) : ImageSegmentationRepository {
    override suspend fun prepareSegmentationModule() {
        moduleInstaller.ensureInstalled()
    }

    /**
     * 서버 토핑의 `imageUrl`은 `https://`다 — `ContentResolver`는 `content://`·`file://`만
     * 열 수 있어 그대로 넘기면 항상 실패한다(`#274`). 원격 주소면 직접 받아 디코드하고,
     * 그 외(기기 로컬 캐시)는 기존 `ContentResolver` 경로를 그대로 쓴다.
     */
    override suspend fun decodeImage(uri: String): BitmapWrapper {
        val bitmap: Bitmap = if (uri.isRemoteImageUrl()) {
            val bytes = remoteImageDownloadDataSource.download(uri)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IOException("이미지를 디코드하지 못했다 - uri: $uri")
        } else {
            context.contentResolver.decodeUriToBitmap(uri.toUri())
        }

        return bitmap.toAndroidBitmap()
    }

    private fun String.isRemoteImageUrl(): Boolean = startsWith("http://") || startsWith("https://")

    override suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> {
        val bitmap: Bitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData() ?: return Result.failure(
            SegmentationException.ImageNotFound(null),
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val result = runSegmenter(multipleSubjectOptions(), image).getOrElse { return Result.failure(it) }

        val harvested = try {
            withContext(Dispatchers.Default) { harvestSubjects(result.subjects, bitmap, projection = null) }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        if (harvested.isEmpty()) {
            repositoryLogger.i { "세그멘테이션: 후처리 대상이 0건이다. 전경 마스크 폴백으로 내려간다" }
            return segmentForeground(image, bitmap)
        }

        val reverted = harvested.count { it.reverted }
        if (reverted > 0) {
            // 후처리는 개선 수단이지 후보를 없앨 권한이 아니다
            repositoryLogger.i { "세그멘테이션 후처리: ${harvested.size}개 중 ${reverted}개를 후처리 이전 후보로 되돌린다" }
        }

        val candidates = try {
            withContext(Dispatchers.Default) { filterCandidates(harvested.map { it.candidate }) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(SegmentationException.Process(e))
        }

        // 전멸이 아니라 일부만 걸러진 경우도 남긴다 — 전멸 로그만 있으면 극단값에서만 판정된다
        repositoryLogger.i { "세그멘테이션 필터 통과 ${candidates.size}/${harvested.size}" }

        if (candidates.isNotEmpty()) return Result.success(candidates)

        repositoryLogger.i { "세그멘테이션: 필터가 후보 ${harvested.size}개를 전부 걸러 냈다. 전경 마스크 폴백으로 내려간다" }
        return segmentForeground(image, bitmap)
    }

    /**
     * 전경 마스크로 후보 한 개를 만든다. 다중 후보가 하나도 안 남았을 때의 폴백이다.
     *
     * ⚠️ **세그멘테이션을 한 번 더 돌린다.** 전경 마스크 옵션을 다중 후보 옵션과 함께 켜면
     * ML Kit 모듈이 `SIGSEGV` 로 죽어서(2026-08-23 실기기 확인, Galaxy A35) 두 옵션을 한
     * 요청에 실을 수 없다. 대신 이 비용은 후보가 0건인 사진에서만 든다.
     *
     * 모듈이 없으면 위로 올린다. 그 밖의 실패는 종전처럼 「인식된 대상 없음」으로 접는다 — 모두 올리면
     * 1차의 처리 실패가 빈 목록에서 예외로 분류가 바뀌어 그 사진의 재시도가 회복 경로로 못 간다.
     */
    private suspend fun segmentForeground(
        image: InputImage,
        origin: Bitmap,
    ): Result<List<SegmentationCandidate>> {
        val result = runSegmenter(foregroundOptions(), image).getOrElse { cause ->
            return if (cause is SegmentationException.ModuleNotReady) {
                Result.failure(cause)
            } else {
                Result.success(emptyList())
            }
        }
        val mask = result.foregroundConfidenceMask ?: return Result.success(emptyList())

        return try {
            val harvest = withContext(Dispatchers.Default) {
                harvestForeground(mask, origin.width, origin.height, origin, projection = null, hintThreshold = null)
            }
            Result.success(harvest.candidates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun recoverCandidates(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>> {
        val origin = (bitmapWrapper as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        // ⚠️ runSegmenter 는 Tasks.await 블로킹 대기라 추론 도중에는 끊기지 않는다. 상한은 진행 중인 추론 하나가
        // 끝난 뒤에 걸린다
        return withTimeoutOrNull(RECOVERY_TIMEOUT_MS) { runRecoveryLadder(origin) } ?: run {
            repositoryLogger.w { "회복: 대기 상한 ${RECOVERY_TIMEOUT_MS}ms 를 넘겨 빈 결과로 접는다" }
            Result.success(emptyList())
        }
    }

    private suspend fun runRecoveryLadder(origin: Bitmap): Result<List<SegmentationCandidate>> {
        val job = currentCoroutineContext().job
        var hint: DetectionBounds? = null
        var hintTransform: RecoveryTransform? = null

        val normalize = normalizeStage(origin.width, origin.height, RECOVERY_APPLY_CONTRAST)
        if (normalize == null) {
            repositoryLogger.i { "회복 1단계: 목표 치수가 원본과 같고 대비가 꺼져 있어 무동작 가드로 건너뛴다" }
        } else {
            when (val outcome = runStage("1단계", origin, normalize)) {
                is StageOutcome.Found -> return Result.success(outcome.candidates)

                is StageOutcome.Aborted -> return Result.failure(outcome.cause)

                is StageOutcome.Empty -> {
                    hint = outcome.hint
                    hintTransform = normalize.transform
                }
            }
        }
        job.ensureActive()

        val crop = focusCrop(origin.width, origin.height, hint, hintTransform)
        val percent = cropAreaPercent(crop, origin.width, origin.height)
        val focus = focusStage(origin.width, origin.height, crop, RECOVERY_APPLY_CONTRAST)
        if (focus == null) {
            repositoryLogger.i { "회복 2단계: 크롭이 원본의 $percent% 라 수축 가드로 건너뛴다, 힌트 ${hint != null}" }
            return Result.success(emptyList())
        }
        repositoryLogger.i { "회복 2단계: 크롭이 원본의 $percent%, 힌트 ${hint != null}" }

        return when (val outcome = runStage("2단계", origin, focus)) {
            is StageOutcome.Found -> Result.success(outcome.candidates)
            is StageOutcome.Aborted -> Result.failure(outcome.cause)
            is StageOutcome.Empty -> Result.success(emptyList())
        }
    }

    /**
     * 한 단계를 돌린다. `ModuleNotReady` 만 사다리를 멈추고, 그 밖의 실패는 이 단계만 포기한다.
     *
     * 단계가 끝나면 ML Kit 결과를 놓고 힌트 좌표 넷만 들고 나온다. 결과를 다음 단계까지 붙들면 피크가 커지고,
     * 네이티브 신뢰도 버퍼가 새 세그멘터를 연 뒤에도 유효한지에 기대게 된다.
     */
    private suspend fun runStage(
        name: String,
        origin: Bitmap,
        stage: RecoveryStage,
    ): StageOutcome {
        val startedAt = SystemClock.elapsedRealtime()
        val plate = try {
            withContext(Dispatchers.Default) { normalizeForDetection(origin, stage) }
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 $name: 검출 판을 만들다 메모리로 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 $name: 검출 판을 만들다 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        }

        try {
            val image = InputImage.fromBitmap(plate.bitmap, 0)
            val whole = SegmentationBounds(0, 0, origin.width, origin.height)
            val source = stage.cropRect ?: whole
            val projection = DetectionProjection(stage.transform, clip = source)
            val capped = stage.targetSize.width < source.width

            val multi = runSegmenter(multipleSubjectOptions(), image).getOrElse { cause ->
                return if (cause is SegmentationException.ModuleNotReady) {
                    StageOutcome.Aborted(cause)
                } else {
                    StageOutcome.Empty(hint = null)
                }
            }
            val harvested = withContext(Dispatchers.Default) { harvestSubjects(multi.subjects, origin, projection) }
            val candidates = withContext(Dispatchers.Default) { filterCandidates(harvested.map { it.candidate }) }
            val relaxed = harvested.count { it.candidate.passesRelaxedFloor() }

            repositoryLogger.i {
                "회복 $name: 목표 ${stage.targetSize.width}x${stage.targetSize.height}, 상한 걸림 $capped, " +
                    "필터 통과 ${candidates.size}/${harvested.size}(1/$RELAXED_FLOOR_LOG_DIVISOR 하한이었다면 $relaxed), " +
                    "소요 ${SystemClock.elapsedRealtime() - startedAt}ms"
            }
            if (candidates.isNotEmpty()) return StageOutcome.Found(candidates)

            val foreground = runSegmenter(foregroundOptions(), image).getOrElse { cause ->
                return if (cause is SegmentationException.ModuleNotReady) {
                    StageOutcome.Aborted(cause)
                } else {
                    StageOutcome.Empty(hint = null)
                }
            }
            val mask = foreground.foregroundConfidenceMask ?: return StageOutcome.Empty(hint = null)
            val harvest = withContext(Dispatchers.Default) {
                harvestForeground(
                    mask = mask,
                    maskWidth = plate.bitmap.width,
                    maskHeight = plate.bitmap.height,
                    origin = origin,
                    projection = projection,
                    hintThreshold = AlphaPostProcessOptions().binaryThreshold,
                )
            }

            // 폴백 후보는 필터를 거치지 않는다 — 1차 경로와 같은 규칙이다
            repositoryLogger.i {
                "회복 $name: 폴백 후보 ${harvest.candidates.size}, 힌트 ${harvest.hint != null}, " +
                    "소요 ${SystemClock.elapsedRealtime() - startedAt}ms"
            }

            return if (harvest.candidates.isNotEmpty()) {
                StageOutcome.Found(harvest.candidates)
            } else {
                StageOutcome.Empty(harvest.hint)
            }
        } catch (e: OutOfMemoryError) {
            repositoryLogger.w(e) { "회복 $name: 메모리로 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            repositoryLogger.w(e) { "회복 $name: 실패해 이 단계를 포기한다" }
            return StageOutcome.Empty(hint = null)
        } finally {
            if (plate.ownedByUs) plate.bitmap.recycle()
        }
    }

    private fun multipleSubjectOptions(): SubjectSegmenterOptions = SubjectSegmenterOptions
        .Builder()
        .enableMultipleSubjects(
            SubjectSegmenterOptions.SubjectResultOptions
                .Builder()
                .enableSubjectBitmap()
                .build(),
        ).build()

    private fun foregroundOptions(): SubjectSegmenterOptions = SubjectSegmenterOptions
        .Builder()
        .enableForegroundConfidenceMask()
        .build()

    /** 로그 전용. 알파 합은 칠해진 픽셀 수의 255 배다 */
    private fun SegmentationCandidate.passesRelaxedFloor(): Boolean {
        val floor = SubjectCoverage.floorPixels(canvasWidth.toLong() * canvasHeight) / RELAXED_FLOOR_LOG_DIVISOR
        return coverageAlphaSum >= floor * 255L
    }

    private sealed interface StageOutcome {
        class Found(val candidates: List<SegmentationCandidate>) : StageOutcome

        /** 다음 단계가 쓸 힌트. 후보가 없어도 힌트는 있을 수 있다 */
        class Empty(val hint: DetectionBounds?) : StageOutcome

        /** 남은 단계도 같은 이유로 실패한다 */
        class Aborted(val cause: Throwable) : StageOutcome
    }

    /**
     * 모델은 APK 가 아니라 Play 서비스가 내려주는 optional module 이라, 받기 전에 process 하면 실패한다.
     */
    private suspend fun runSegmenter(
        options: SubjectSegmenterOptions,
        image: InputImage,
    ): Result<SubjectSegmentationResult> {
        val outcome = moduleInstaller.ensureInstalled()
        if (outcome != ModuleInstallOutcome.Ready) {
            repositoryLogger.w { "[MLKIT-MODULE] 모듈 미준비($outcome)로 process 를 건너뛴다" }
            return Result.failure(SegmentationException.ModuleNotReady(null))
        }

        val segmenter = try {
            SubjectSegmentation.getClient(options)
        } catch (e: Exception) {
            return Result.failure(SegmentationException.ClientInit(e))
        }

        return try {
            segmenter.use { segmenter ->
                withContext(Dispatchers.IO) {
                    Result.success(Tasks.await(segmenter.process(image)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e.toSegmentationException())
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
                        sourceLongSide = SourceLongSide(
                            maxOf(candidate.canvasWidth, candidate.canvasHeight),
                        ),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
    }

    override suspend fun saveBitmap(bitmapWrapper: BitmapWrapper): Result<String> {
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
     * 모듈 다운로드가 끝나지 않아 실패한 경우와 그 외 처리 실패를 구분한다.
     * [Tasks.await] 는 원인을 [ExecutionException] 으로 감싸서 던지므로 한 겹 벗겨서 확인한다.
     */
    private fun Throwable.toSegmentationException(): SegmentationException {
        val cause = (this as? ExecutionException)?.cause ?: this

        repositoryLogger.w(cause) {
            "[MLKIT-MODULE] process 실패 ${cause::class.simpleName}, " +
                "MlKit 오류 코드 ${(cause as? MlKitException)?.errorCode}"
        }

        return if (cause is MlKitException && cause.errorCode == MlKitException.UNAVAILABLE) {
            SegmentationException.ModuleNotReady(cause)
        } else {
            SegmentationException.Process(cause)
        }
    }
}

private const val RECOVERY_TIMEOUT_MS = 30_000L

/** 조건부 항목이다. 로그가 대비 스트레치를 철회하면 여기만 끈다 — 그러면 1단계 무동작 가드가 의미를 갖는다 */
private const val RECOVERY_APPLY_CONTRAST = true
