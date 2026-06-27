package com.teamyg.parfait.feature.segmentation.impl.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.teamyg.parfait.feature.segmentation.impl.screen.decodeUriToBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SegmentationResult(
    val overlayBitmap: Bitmap,
    val subjectImagePath: String,
)

class ImageSegmentationRepository
@Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun decodeImage(uri: String): Bitmap = withContext(Dispatchers.IO) {
        decodeUriToBitmap(context.contentResolver, Uri.parse(uri))
    }

    suspend fun segmentImage(bitmap: Bitmap): SegmentationResult {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = SubjectSegmenterOptions
            .Builder()
            .enableForegroundConfidenceMask()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)

        val result = withContext(Dispatchers.IO) {
            Tasks.await(segmenter.process(image))
        }

        return withContext(Dispatchers.Default) {
            val foregroundMask = result.foregroundConfidenceMask!!
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

            SegmentationResult(
                overlayBitmap = overlayBitmap,
                subjectImagePath = file.absolutePath,
            )
        }
    }
}
