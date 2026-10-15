package com.teamyg.parfait.feature.groups.canvas.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import java.io.File

/** 불투명 단색 PNG 를 캐시 디렉터리에 쓰고 절대경로를 돌려준다 */
internal fun writeTestToppingPng(
    context: Context,
    name: String,
    widthPx: Int = 200,
    heightPx: Int = 200,
): String {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.RED)

    val file = File(context.cacheDir, name)
    file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
    bitmap.recycle()

    return file.absolutePath
}
