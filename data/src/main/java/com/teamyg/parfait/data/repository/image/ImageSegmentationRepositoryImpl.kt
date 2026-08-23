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
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import com.teamyg.parfait.domain.exception.SegmentationException
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

    override suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<SegmentationResult> {
        val bitmap: Bitmap = (bitmapWrapper as? AndroidBitmap)?.getRawData() ?: return Result.failure(
            SegmentationException.ImageNotFound(null),
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val options = SubjectSegmenterOptions
            .Builder()
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
            // 이 블록은 위 try 밖이라, 안에서 예외가 그대로 새어나가면 toSegmentationException
            // 매핑을 타지 못하고 호출부(SegmentationViewModel init)의 코루틴을 그대로 죽인다.
            // saveToCacheAsPng() 의 IOException(저장 공간 부족·캐시 회수 등)을 값으로 감싼다
            try {
                // 마스크가 없으면 잘라낼 근거가 없다. 실패도 값으로 돌려준다
                val foregroundMask = result.foregroundConfidenceMask
                    ?: return@withContext Result.failure(SegmentationException.Process(null))

                val width = bitmap.width
                val height = bitmap.height

                // InputImage.fromBitmap(bitmap, 0) 이라 지금은 치수가 같지만 그 일치가 계약으로
                // 적혀 있지 않다. 어긋난 채로 읽으면 엉뚱한 자리를 객체로 오려낸다.
                // absolute get(index) 는 capacity 가 아니라 limit 을 경계로 삼으므로(넘으면
                // IndexOutOfBoundsException), 남은 유효 구간을 뜻하는 remaining() 으로 비교한다
                if (foregroundMask.remaining() != width * height) {
                    return@withContext Result.failure(SegmentationException.Process(null))
                }

                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val subjectBounds = maskSubjectPixels(pixels, foregroundMask, width, height)

                val subjectBitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

                // subjectBitmap 이 살아 있는 구간을 finally 로 감싸서, 저장 도중 실패해도
                // 전체 해상도 비트맵이 GC 전까지 붙들려 있지 않게 한다
                try {
                    val file = subjectBitmap.saveToCacheAsPng()

                    // 미리보기·배치는 투명 여백 없이 실제 객체 크기만 필요하므로, 이미 알고 있는 bounding box 로 바로 잘라 둔다
                    val trimmedFile = subjectBounds?.let { bounds ->
                        val trimmedBitmap = Bitmap.createBitmap(
                            subjectBitmap,
                            bounds.left,
                            bounds.top,
                            bounds.width,
                            bounds.height,
                        )
                        val saved = trimmedBitmap.saveToCacheAsPng()
                        if (trimmedBitmap !== subjectBitmap) trimmedBitmap.recycle()
                        saved
                    }

                    val segmentationResult = SegmentationResult(
                        subjectImagePath = file.absolutePath,
                        trimmedSubjectImagePath = (trimmedFile ?: file).absolutePath,
                        subjectBounds = subjectBounds,
                    )

                    Result.success(segmentationResult)
                } finally {
                    subjectBitmap.recycle()
                }
            } catch (e: CancellationException) {
                // 취소는 실패가 아니다 — 값으로 접으면 상위로 전파되지 않아 취소된 흐름이 계속 돈다
                throw e
            } catch (e: Exception) {
                Result.failure(SegmentationException.Process(e))
            }
        }
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
                        // 이 필드는 Task 3 이 걷는다. 지금은 아직 있어서 넘겨야 컴파일된다
                        subjectBounds = candidate.bounds,
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
