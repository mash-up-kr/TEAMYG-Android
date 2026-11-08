package com.teamyg.parfait.feature.camera.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect as AndroidRect
import android.os.Build
import androidx.compose.ui.unit.IntRect
import java.io.File

/** 크롭과 회전을 `createBitmap` 한 번으로 처리해 저장한다. `cropRect`가 null 이면 전체를 회전만 한다. */
internal fun saveCroppedCapture(
    captured: Bitmap,
    cropRect: IntRect?,
    rotationDegrees: Int,
    file: File,
) {
    captured.rotated(rotationDegrees, cropRect).compressTo(file)
}

/**
 * JPEG 에서 [cropRect] 영역만 디코드해 회전한 뒤 저장한다. 전체 프레임을 디코드하지 않아 [saveCroppedCapture]보다 빠르다.
 */
internal fun saveCroppedJpeg(
    jpeg: ByteArray,
    cropRect: IntRect?,
    rotationDegrees: Int,
    file: File,
) {
    val decoded = if (cropRect == null) {
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
    } else {
        val decoder = newRegionDecoder(jpeg)
        try {
            decoder.decodeRegion(AndroidRect(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom), null)
        } finally {
            decoder.recycle()
        }
    } ?: error("JPEG 디코드에 실패했다")

    val rotated = decoded.rotated(rotationDegrees)
    if (rotated !== decoded) decoded.recycle()

    rotated.compressTo(file)
}

private fun newRegionDecoder(jpeg: ByteArray): BitmapRegionDecoder {
    val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size, false)
    }
    return decoder ?: error("JPEG 영역 디코더를 만들지 못했다")
}

private fun Bitmap.rotated(
    degrees: Int,
    region: IntRect? = null,
): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val bounds = region ?: IntRect(0, 0, width, height)
    return Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width, bounds.height, matrix, true)
}

private fun Bitmap.compressTo(file: File) {
    file.outputStream().use { output ->
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    }
}

private const val JPEG_QUALITY = 95
