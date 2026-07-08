package com.teamyg.parfait.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.teamyg.parfait.core.designsystem.theme.colors.YGColorScheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGSemanticColorDefaults
import com.teamyg.parfait.core.designsystem.utils.createPreviewBitmap

internal val LocalYGColorScheme: ProvidableCompositionLocal<YGColorScheme> = staticCompositionLocalOf {
    error("Not Init ColorScheme")
}

object YGTheme {
    val colorScheme: YGColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalYGColorScheme.current
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun YGCustomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: YGColorScheme = if (darkTheme) YGSemanticColorDefaults.YGDarkColorScheme else YGSemanticColorDefaults.YGLightColorScheme
    val asyncImagePreviewHandler: AsyncImagePreviewHandler = remember { AsyncImagePreviewHandler { createPreviewBitmap() } }

    CompositionLocalProvider(
        LocalAsyncImagePreviewHandler provides asyncImagePreviewHandler,
        LocalYGColorScheme provides colorScheme,
        content = content,
    )
}
