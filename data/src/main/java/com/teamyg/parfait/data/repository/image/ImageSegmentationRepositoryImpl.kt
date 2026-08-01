package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import com.teamyg.parfait.core.util.android.extension.decodeUriToBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationBounds
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
            val foregroundMask = result.foregroundConfidenceMask ?: error("foregroundConfidenceMask가 null입니다.")
            val subjectColors = IntArray(image.width * image.height)

            // 객체 픽셀의 최소/최대 좌표를 모아 bounding box 를 만든다
            var left = Int.MAX_VALUE
            var top = Int.MAX_VALUE
            var right = -1
            var bottom = -1

            for (i in 0 until image.width * image.height) {
                if (foregroundMask[i] > 0.5f) {
                    val x = i % image.width
                    val y = i / image.width

                    subjectColors[i] = bitmap.getPixel(x, y)

                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }

            // 감지된 픽셀이 하나도 없으면 bounding box 도 없다
            val subjectBounds = if (left <= right && top <= bottom) {
                SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
            } else {
                null
            }

            val subjectBitmap = Bitmap.createBitmap(subjectColors, image.width, image.height, Bitmap.Config.ARGB_8888)

            val file = File(context.cacheDir, "parfait_${System.currentTimeMillis()}.png")
            withContext(Dispatchers.IO) {
                file.outputStream().use { subjectBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            subjectBitmap.recycle()

            val result = SegmentationResult(
                subjectImagePath = file.absolutePath,
                subjectBounds = subjectBounds,
            )

            return@withContext Result.success(result)
        }
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
