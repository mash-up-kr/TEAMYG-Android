package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.teamyg.parfait.data.utils.image.SEGMENTATION_CACHE_DIR_NAME
import com.teamyg.parfait.data.utils.image.clearFiles
import com.teamyg.parfait.data.utils.image.filterCandidates
import com.teamyg.parfait.data.utils.image.harvestForeground
import com.teamyg.parfait.data.utils.image.harvestSubjects
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.image.SourceLongSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val multipleSubjectOptions = SubjectSegmenterOptions
            .Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions
                    .Builder()
                    .enableSubjectBitmap()
                    .build(),
            ).build()

        val result = runSegmenter(multipleSubjectOptions, image).getOrElse { return Result.failure(it) }

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
        val options = SubjectSegmenterOptions
            .Builder()
            .enableForegroundConfidenceMask()
            .build()

        val result = runSegmenter(options, image).getOrElse { cause ->
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
