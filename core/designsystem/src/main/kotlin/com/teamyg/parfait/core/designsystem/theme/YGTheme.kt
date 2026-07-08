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
import com.teamyg.parfait.core.designsystem.theme.layout.YGLayout
import com.teamyg.parfait.core.designsystem.theme.layout.YGLayoutDefaults
import com.teamyg.parfait.core.designsystem.theme.shapes.YGShapes
import com.teamyg.parfait.core.designsystem.theme.shapes.YGShapesDefaults
import com.teamyg.parfait.core.designsystem.theme.typography.YGTypography
import com.teamyg.parfait.core.designsystem.theme.typography.YGTypographyDefaults
import com.teamyg.parfait.core.designsystem.utils.createPreviewBitmap

internal val LocalYGColorScheme: ProvidableCompositionLocal<YGColorScheme> = staticCompositionLocalOf {
    error("Not Init ColorScheme")
}

internal val LocalYGTypography: ProvidableCompositionLocal<YGTypography> = staticCompositionLocalOf {
    error("Not Init Typography")
}

internal val LocalYGShapes: ProvidableCompositionLocal<YGShapes> = staticCompositionLocalOf {
    error("Not Init Shapes")
}

internal val LocalYGLayout: ProvidableCompositionLocal<YGLayout> = staticCompositionLocalOf {
    error("Not Init Layout")
}

object YGTheme {
    val colorScheme: YGColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalYGColorScheme.current

    val typography: YGTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalYGTypography.current

    val shapes: YGShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalYGShapes.current

    val layout: YGLayout
        @Composable
        @ReadOnlyComposable
        get() = LocalYGLayout.current
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
        LocalYGTypography provides YGTypographyDefaults.YGDefaultTypography,
        LocalYGShapes provides YGShapesDefaults.YGDefaultShapes,
        LocalYGLayout provides YGLayoutDefaults.YGDefaultLayout,
        content = content,
    )
}
