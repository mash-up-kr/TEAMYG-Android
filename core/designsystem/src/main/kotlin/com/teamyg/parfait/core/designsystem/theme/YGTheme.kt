package com.teamyg.parfait.core.designsystem.theme

import android.graphics.Canvas
import android.graphics.Color
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
import com.teamyg.parfait.core.designsystem.theme.colors.YGSemanticColors

private val YGDarkColorScheme = darkColorScheme(
    primary = YGSemanticColors.Primary,
    secondary = YGSemanticColors.Secondary,
    tertiary = YGSemanticColors.Tertiary,
)

private val YGLightColorScheme = lightColorScheme(
    primary = YGSemanticColors.Primary,
    secondary = YGSemanticColors.Secondary,
    tertiary = YGSemanticColors.Tertiary,
)

@OptIn(ExperimentalCoilApi::class)
@Composable
fun YGMaterialTheme(
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

        darkTheme -> YGDarkColorScheme

        else -> YGLightColorScheme
    }

    CompositionLocalProvider(
        LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler {
            createBitmap(
                width = 300,
                height = 300,
                backgroundColor = Color.LTGRAY,
                textColor = Color.BLACK,
                textSize = 30f,
            )
        },
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
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
