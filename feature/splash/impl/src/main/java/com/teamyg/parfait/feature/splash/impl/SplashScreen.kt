package com.teamyg.parfait.feature.splash.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview
import com.teamyg.parfait.core.ui.R as CoreUiR

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.splash_icon),
            contentDescription = null,
        )
    }
}

@YGPreview
@Composable
private fun SplashScreenPreview() = PreviewBox {
    SplashScreen(
        modifier = Modifier.fillMaxSize(),
    )
}
