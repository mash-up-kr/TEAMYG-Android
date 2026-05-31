package com.teamyg.designsystem.theme

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import coil3.BitmapImage
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler

private val TempDarkColorScheme = darkColorScheme(
    primary = YGAtomicColors.TempPurple80,
    secondary = YGAtomicColors.TempPurpleGrey80,
    tertiary = YGAtomicColors.TempPink80,
)

private val TempLightColorScheme = lightColorScheme(
    primary = YGAtomicColors.TempPurple40,
    secondary = YGAtomicColors.TempPurpleGrey40,
    tertiary = YGAtomicColors.TempPink40,
)

@OptIn(ExperimentalCoilApi::class)
@Composable
fun TempYGMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> TempDarkColorScheme

        else -> TempLightColorScheme
    }

    CompositionLocalProvider(
        LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler {
            createBitmap(
                width = 300,
                height = 300,
                backgroundColor = android.graphics.Color.LTGRAY,
                textColor = android.graphics.Color.BLACK,
                textSize = 30f,
            )
        },
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YGTypography.TempTypography,
            content = content,
        )
    }
}

fun createBitmap(
    width: Int,
    height: Int,
    backgroundColor: Int,
    textColor: Int,
    textSize: Float,
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
