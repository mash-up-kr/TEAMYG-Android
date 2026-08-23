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
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.exception.SegmentationException
import com.teamyg.parfait.domain.model.SegmentationBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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

        val options = SubjectSegmenterOptions
            .Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions
                    .Builder()
                    .enableSubjectBitmap()
                    .build(),
            )
            // 후보가 0건일 때 폴백이 쓴다
            .enableForegroundConfidenceMask()
            .build()

        val segmenter = try {
            SubjectSegmentation.getClient(options)
        } catch (e: Exception) {
            return Result.failure(SegmentationException.ClientInit(e))
        }

        val result = try {
            segmenter.use { segmenter ->
                // 모델은 APK 가 아니라 Play 서비스가 내려주는 optional module 이라, 받기 전에 process 하면 실패한다
                if (!ensureModuleInstalled(segmenter)) {
                    return Result.failure(SegmentationException.ModuleNotReady(null))
                }

                withContext(Dispatchers.IO) {
                    Tasks.await(segmenter.process(image))
                }
            }
        } catch (e: Exception) {
            return Result.failure(e.toSegmentationException())
        }

        return withContext(Dispatchers.Default) {
            try {
                val candidates = filterCandidates(result.toCandidates(bitmap))

                // 필터가 전부 걸러 낸 경우도 폴백을 태운다. 이 갈래를 빼면 지금 잘 되던 사진이
                // 다중 전환 이후 실패로 바뀐다
                val resolved = candidates.ifEmpty { result.fallbackCandidates(bitmap) }

                Result.success(resolved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
    }

    /**
     * `getBitmap()` 은 널을 돌려줄 수 있다 — `enableSubjectBitmap()` 을 켰다는 이유로 비널을
     * 단정하지 않는다. 판이 없는 후보는 고를 수 없으므로 버린다.
     */
    private fun SubjectSegmentationResult.toCandidates(origin: Bitmap): List<SegmentationCandidate> =
        subjects.mapNotNull { subject ->
            val subjectBitmap = subject.bitmap ?: return@mapNotNull null

            SegmentationCandidate(
                // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다
                bounds = SegmentationBounds(
                    left = subject.startX,
                    top = subject.startY,
                    right = subject.startX + subject.width,
                    bottom = subject.startY + subject.height,
                ),
                bitmap = subjectBitmap.toAndroidBitmap(),
                canvasWidth = origin.width,
                canvasHeight = origin.height,
            )
        }

    /**
     * 후보가 하나도 안 남았을 때 전경 마스크로 한 개를 만든다.
     *
     * 이 경로가 실제로 도달 가능한지는 아직 확인하지 못했다(`synthesis/open-questions.md` OQ-P-268).
     */
    private fun SubjectSegmentationResult.fallbackCandidates(origin: Bitmap): List<SegmentationCandidate> {
        val foregroundMask = foregroundConfidenceMask ?: return emptyList()

        val width = origin.width
        val height = origin.height

        // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
        // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
        // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
        // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
        if (foregroundMask.remaining() != width * height) return emptyList()

        val pixels = IntArray(width * height)
        origin.getPixels(pixels, 0, width, 0, 0, width, height)

        val bounds = maskSubjectPixels(pixels, foregroundMask, width, height) ?: return emptyList()

        val masked = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

        // 잘라내야 한다 — 후보의 비트맵은 bounds 크기라는 것이 저장 쪽 전제다.
        // 원본 크기 판을 그대로 실으면 (left, top) 만큼 밀려 그려진다
        val trimmed = Bitmap.createBitmap(masked, bounds.left, bounds.top, bounds.width, bounds.height)

        // createBitmap 은 자를 것이 없으면 원본 인스턴스를 그대로 돌려준다.
        // 그때 회수하면 방금 만든 판이 사라진다
        if (trimmed !== masked) masked.recycle()

        return listOf(
            SegmentationCandidate(
                bounds = bounds,
                bitmap = trimmed.toAndroidBitmap(),
                canvasWidth = width,
                canvasHeight = height,
            ),
        )
    }

    override suspend fun persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult> {
        val trimmed: Bitmap = (candidate.bitmap as? AndroidBitmap)?.getRawData()
            ?: return Result.failure(SegmentationException.ImageNotFound(null))

        return withContext(Dispatchers.Default) {
            try {
                val trimmedFile = trimmed.saveToCacheAsPng()

                // 원본과 같은 좌표계의 판. 편집 화면이 원본 위에 픽셀로 겹쳐 그린다
                val canvas = Bitmap.createBitmap(
                    candidate.canvasWidth,
                    candidate.canvasHeight,
                    Bitmap.Config.ARGB_8888,
                )

                val subjectFile = try {
                    // 스케일하지 않고 그대로 얹는다 — ML Kit 가 준 치수와 bounds 가 어긋나더라도
                    // 그림이 찌그러지지는 않게 한다
                    Canvas(canvas).drawBitmap(
                        trimmed,
                        candidate.bounds.left.toFloat(),
                        candidate.bounds.top.toFloat(),
                        null,
                    )
                    canvas.saveToCacheAsPng()
                } finally {
                    canvas.recycle()
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
