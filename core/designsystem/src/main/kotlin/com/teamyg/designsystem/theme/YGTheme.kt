package com.teamyg.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun TempYGMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> TempDarkColorScheme

        else -> TempLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = YGTypography.TempTypography,
        content = content
    )
}
