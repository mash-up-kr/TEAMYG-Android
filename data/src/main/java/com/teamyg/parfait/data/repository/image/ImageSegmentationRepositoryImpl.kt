package com.teamyg.parfait.data.repository.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.teamyg.parfait.core.util.android.extension.decodeUriToBitmap
import com.teamyg.parfait.core.util.jvm.model.BitmapWrapper
import com.teamyg.parfait.domain.model.SegmentationResult
import com.teamyg.parfait.domain.repository.image.ImageSegmentationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.teamyg.parfait.core.util.android.extension.toAndroidBitmap
import com.teamyg.parfait.core.util.android.model.AndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
            exception = NullPointerException("bitmap is null"),
        )

        val image = InputImage.fromBitmap(bitmap, 0)

        val options = SubjectSegmenterOptions
            .Builder()
            .enableForegroundConfidenceMask()
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
        val result = withContext(Dispatchers.IO) {
            segmenter.use { segmenter ->
                Tasks.await(segmenter.process(image))
            }
        }

        return withContext(Dispatchers.Default) {
            val foregroundMask = result.foregroundConfidenceMask ?: error("foregroundConfidenceMask가 null입니다.")
            val overlayColors = IntArray(image.width * image.height)
            val subjectColors = IntArray(image.width * image.height)

            for (i in 0 until image.width * image.height) {
                if (foregroundMask[i] > 0.5f) {
                    overlayColors[i] = Color.argb(128, 255, 0, 255)
                    subjectColors[i] = bitmap.getPixel(i % image.width, i / image.width)
                }
            }

            val overlayBitmap = Bitmap.createBitmap(overlayColors, image.width, image.height, Bitmap.Config.ARGB_8888)
            val subjectBitmap = Bitmap.createBitmap(subjectColors, image.width, image.height, Bitmap.Config.ARGB_8888)

            val file = File(context.cacheDir, "parfait_${System.currentTimeMillis()}.png")
            withContext(Dispatchers.IO) {
                file.outputStream().use { subjectBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            subjectBitmap.recycle()

            val result = SegmentationResult(
                bitmap = overlayBitmap.toAndroidBitmap(),
                subjectImagePath = file.absolutePath,
            )

            return@withContext Result.success(result)
        }
    }
}
