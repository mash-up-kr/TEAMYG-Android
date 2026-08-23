package com.teamyg.parfait.core.util.android.extension

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger

fun ContentResolver.decodeUriToBitmap(uri: Uri): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val source = ImageDecoder.createSource(this, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
        decoder.setTargetSize(info.size.width, info.size.height)
    }
} else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media
        .getBitmap(this, uri)
        .rotatedToUpright(this, uri)
}

/** [uri] 가 가리키는 내용 전부. `content://` 든 `file://` 든 열 수 없으면 던진다 */
fun ContentResolver.readBytes(uri: Uri): ByteArray = openInputStream(uri).use { input ->
    requireNotNull(input) { "입력 스트림을 열 수 없다 - uri: $uri" }

    input.readBytes()
}

/**
 * EXIF 회전을 픽셀에 적용한다.
 *
 * 왜 API 28 이상에서는 부르지 않는지는
 * `parfait/synthesis/open-questions.md` OQ-P-280 에 있다.
 */
private fun Bitmap.rotatedToUpright(
    resolver: ContentResolver,
    uri: Uri,
): Bitmap {
    val degrees = resolver.readExifDegrees(uri)
    if (degrees == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)

    // 각도가 0 이 아니라 언제나 새 인스턴스다. 전체 해상도 판 둘이 함께 살지 않게 여기서 닫는다
    recycle()

    return rotated
}

/**
 * [ImageDecoder.createSource] 가 스트림을 소비하므로 EXIF 는 uri 를 한 번 더 열어서 읽는다.
 *
 * 못 읽으면 0 이다 — 태그가 깨진 것과 이미지를 못 연 것은 다른 사건이라 디코드를 실패시키지
 * 않는다. 다만 이 갈래가 상시 참이 되면 보정이 조용히 무효가 되므로 남긴다.
 */
private fun ContentResolver.readExifDegrees(uri: Uri): Int = try {
    openInputStream(uri).use { input ->
        if (input == null) {
            0
        } else {
            exifOrientationToDegrees(
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                ),
            )
        }
    }
} catch (throwable: Exception) {
    coreUtilAndroidLogger.w(throwable) { "EXIF 를 읽지 못해 회전 보정을 건너뛴다 - uri: $uri" }
    0
}
