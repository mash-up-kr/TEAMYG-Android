package com.teamyg.parfait.core.util.extensions

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
