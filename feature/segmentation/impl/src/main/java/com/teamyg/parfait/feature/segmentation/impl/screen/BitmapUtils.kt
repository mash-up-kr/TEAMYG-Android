package com.teamyg.parfait.feature.segmentation.impl.screen

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

internal data class BitmapViewMapping(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

internal fun bitmapToViewMapping(
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): BitmapViewMapping {
    val scale = minOf(
        viewSize.width / bitmapWidth.toFloat(),
        viewSize.height / bitmapHeight.toFloat(),
    )
    val displayedW = bitmapWidth * scale
    val displayedH = bitmapHeight * scale
    val offsetX = (viewSize.width - displayedW) / 2f
    val offsetY = (viewSize.height - displayedH) / 2f
    return BitmapViewMapping(scale, offsetX, offsetY)
}

internal fun mapViewToBitmap(
    point: Offset,
    viewSize: Size,
    bitmapWidth: Int,
    bitmapHeight: Int,
): Pair<Int, Int>? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null
    val mapping = bitmapToViewMapping(viewSize, bitmapWidth, bitmapHeight)
    val x = ((point.x - mapping.offsetX) / mapping.scale).toInt()
    val y = ((point.y - mapping.offsetY) / mapping.scale).toInt()
    if (x !in 0 until bitmapWidth || y !in 0 until bitmapHeight) return null
    return x to y
}

internal fun mapViewToBitmapFloat(
    point: Offset,
    mapping: BitmapViewMapping,
): Offset = Offset(
    x = (point.x - mapping.offsetX) / mapping.scale,
    y = (point.y - mapping.offsetY) / mapping.scale,
)

internal fun decodeUriToBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val source = ImageDecoder.createSource(contentResolver, uri)
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
        decoder.setTargetSize(info.size.width, info.size.height)
    }
} else {
    @Suppress("DEPRECATION")
    MediaStore.Images.Media.getBitmap(contentResolver, uri)
}
