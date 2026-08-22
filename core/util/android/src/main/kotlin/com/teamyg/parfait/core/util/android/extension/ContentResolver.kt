package com.teamyg.parfait.core.util.android.extension

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun ContentResolver.decodeUriToBitmap(uri: Uri): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val source = ImageDecoder.createSource(this, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
        decoder.setTargetSize(info.size.width, info.size.height)
    }
} else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media.getBitmap(this, uri)
}

/** [uri] 가 가리키는 내용 전부. `content://` 든 `file://` 든 열 수 없으면 던진다 */
fun ContentResolver.readBytes(uri: Uri): ByteArray = openInputStream(uri).use { input ->
    requireNotNull(input) { "입력 스트림을 열 수 없다 - uri: $uri" }

    input.readBytes()
}
