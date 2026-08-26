package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.teamyg.parfait.core.util.android.extension.decodeUriToBitmap
import com.teamyg.parfait.core.util.jvm.extension.sumArgbAlpha
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

        val multipleSubjectOptions = SubjectSegmenterOptions
            .Builder()
            .enableMultipleSubjects(
                SubjectSegmenterOptions.SubjectResultOptions
                    .Builder()
                    .enableSubjectBitmap()
                    .build(),
            ).build()

        val result = runSegmenter(multipleSubjectOptions, image).getOrElse { return Result.failure(it) }

        val candidates = try {
            withContext(Dispatchers.Default) { filterCandidates(result.toCandidates(bitmap)) }
        } catch (e: CancellationException) {
            // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
            throw e
        } catch (e: Exception) {
            // 필터·변환이 던질 수 있는 예상 밖 실패를 화면에 토스트로 전달할 수 있게 감싼다
            return Result.failure(SegmentationException.Process(e))
        }

        if (candidates.isNotEmpty()) return Result.success(candidates)

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
            withContext(Dispatchers.Default) { result.toForegroundCandidate(origin) }
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

    /**
     * `getBitmap()` 은 널을 돌려줄 수 있다 — `enableSubjectBitmap()` 을 켰다는 이유로 비널을
     * 단정하지 않는다. 판이 없는 후보는 고를 수 없으므로 버린다.
     */
    private fun SubjectSegmentationResult.toCandidates(origin: Bitmap): List<SegmentationCandidate> =
        subjects.mapNotNull { subject ->
            val subjectBitmap = subject.bitmap ?: return@mapNotNull null

            // 합만 필요하므로 전면 IntArray 를 잡지 않고 행 단위로 읽어 누적한다
            val row = IntArray(subjectBitmap.width)
            var coverage = 0L
            for (y in 0 until subjectBitmap.height) {
                subjectBitmap.getPixels(row, 0, subjectBitmap.width, 0, y, subjectBitmap.width, 1)
                coverage += row.sumArgbAlpha()
            }

            SegmentationCandidate(
                // right·bottom 은 exclusive 라 폭·높이를 그대로 더한다.
                // ML Kit 문서는 getWidth()·getHeight() 가 getBitmap() 의 실제 치수와 같다고
                // 보장하지 않으므로, subject 가 아니라 subjectBitmap 에서 치수를 뽑는다
                bounds = SegmentationBounds(
                    left = subject.startX,
                    top = subject.startY,
                    right = subject.startX + subjectBitmap.width,
                    bottom = subject.startY + subjectBitmap.height,
                ),
                bitmap = subjectBitmap.toAndroidBitmap(),
                canvasWidth = origin.width,
                canvasHeight = origin.height,
                coverageAlphaSum = coverage,
            )
        }

    /**
     * 마스크가 없거나 치수가 어긋나면 빈 목록이다 — 없는 후보를 지어내지 않는다.
     */
    private fun SubjectSegmentationResult.toForegroundCandidate(origin: Bitmap): List<SegmentationCandidate> {
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
                coverageAlphaSum = pixels.sumArgbAlpha(),
            ),
        )
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
