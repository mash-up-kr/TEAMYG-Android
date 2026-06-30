package com.teamyg.parfait.core.util.android.extension

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun Modifier.navigationBarsAndImePadding(): Modifier {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarBottomPadding = WindowInsets.navigationBars
    val imeBottomPadding = WindowInsets.ime

    val insets = WindowInsets(
        left = maxOf(
            a = navigationBarBottomPadding.getLeft(density, layoutDirection),
            b = imeBottomPadding.getLeft(density, layoutDirection),
        ),
        right = maxOf(
            a = navigationBarBottomPadding.getRight(density, layoutDirection),
            b = imeBottomPadding.getRight(density, layoutDirection),
        ),
        top = maxOf(
            a = navigationBarBottomPadding.getTop(density),
            b = imeBottomPadding.getTop(density),
        ),
        bottom = maxOf(
            a = navigationBarBottomPadding.getBottom(density),
            b = imeBottomPadding.getBottom(density),
        ),
    )
    return windowInsetsPadding(insets)
}
