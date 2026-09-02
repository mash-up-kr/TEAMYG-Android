package com.teamyg.parfait.core.designsystem.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import coil3.BitmapImage
import coil3.asImage

internal fun createPreviewBitmap(
    width: Int = 300,
    height: Int = 300,
    backgroundColor: Int = Color.LTGRAY,
    textColor: Int = Color.BLACK,
    textSize: Float = 30f,
): BitmapImage {
    val bitmap = createBitmap(width, height)

    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)

    Paint()
        .apply {
            this.color = textColor
            this.textSize = textSize
            this.textAlign = Paint.Align.CENTER
        }.also { paint ->
            canvas.drawText(
                "Preview Image",
                width / 2f,
                height / 2f,
                paint,
            )
        }

    return bitmap.asImage()
}
